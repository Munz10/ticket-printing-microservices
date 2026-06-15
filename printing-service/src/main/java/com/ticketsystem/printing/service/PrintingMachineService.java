package com.ticketsystem.printing.service;

import com.ticketsystem.printing.exception.ResourceUnavailableException;
import com.ticketsystem.printing.messaging.ResourceEventPublisher;
import com.ticketsystem.printing.model.MachineStatus;
import com.ticketsystem.printing.model.Ticket;
import com.ticketsystem.printing.model.TicketType;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Holds the in-memory state of the ticket printing machine
 * (toner level, paper level, tickets printed) and exposes
 * thread-safe operations on it.
 *
 * This is the service-side equivalent of TicketMachineAdvanced from the
 * original concurrent-ticket-system project, adapted to a request/response
 * (REST) model instead of blocking threads. Low-resource events are
 * published to RabbitMQ for resupply-service to act on.
 */
@Service
public class PrintingMachineService {

    private static final Logger log = LoggerFactory.getLogger(PrintingMachineService.class);

    private final ReentrantLock lock = new ReentrantLock();
    private final AtomicInteger ticketsPrinted = new AtomicInteger(0);
    private final ResourceEventPublisher eventPublisher;
    private final MeterRegistry registry;
    private final Counter resourceUnavailableCounter;

    private volatile int tonerLevel;
    private volatile int paperLevel;
    private boolean tonerLowNotified = false;
    private boolean paperLowNotified = false;

    private final int fullTonerLevel;
    private final int fullPaperTray;
    private final int minimumTonerLevel;
    private final int minimumPaperLevel;
    private final int sheetsPerPack;

    public PrintingMachineService(
            ResourceEventPublisher eventPublisher,
            MeterRegistry registry,
            @Value("${machine.toner.initial}") int initialToner,
            @Value("${machine.paper.initial}") int initialPaper,
            @Value("${machine.toner.full}") int fullTonerLevel,
            @Value("${machine.paper.full}") int fullPaperTray,
            @Value("${machine.toner.minimum}") int minimumTonerLevel,
            @Value("${machine.paper.minimum}") int minimumPaperLevel,
            @Value("${machine.paper.sheets-per-pack}") int sheetsPerPack) {
        this.eventPublisher = eventPublisher;
        this.registry = registry;
        this.tonerLevel = initialToner;
        this.paperLevel = initialPaper;
        this.fullTonerLevel = fullTonerLevel;
        this.fullPaperTray = fullPaperTray;
        this.minimumTonerLevel = minimumTonerLevel;
        this.minimumPaperLevel = minimumPaperLevel;
        this.sheetsPerPack = sheetsPerPack;

        registry.gauge("printing_toner_level", this, m -> m.tonerLevel);
        registry.gauge("printing_paper_level", this, m -> m.paperLevel);
        this.resourceUnavailableCounter = Counter.builder("printing_resource_unavailable_total")
                .description("Number of print requests rejected due to insufficient toner/paper")
                .register(registry);
    }

    /**
     * Print a ticket of the given type. Throws ResourceUnavailableException
     * (mapped to HTTP 409) if there isn't enough toner/paper.
     */
    public Ticket printTicket(TicketType type) {
        lock.lock();
        try {
            if (!type.canPrint(tonerLevel, paperLevel)) {
                resourceUnavailableCounter.increment();
                throw new ResourceUnavailableException(String.format(
                        "Cannot print %s ticket - need toner=%d, paper=%d but have toner=%d, paper=%d",
                        type.getDisplayName(), type.getTonerCost(), type.getPaperCost(), tonerLevel, paperLevel));
            }

            tonerLevel -= type.getTonerCost();
            paperLevel -= type.getPaperCost();
            int number = ticketsPrinted.incrementAndGet();
            registry.counter("printing_tickets_printed_total", "type", type.name()).increment();

            // Fire-and-forget low-resource events, only once per depletion (until refilled)
            if (tonerLevel <= minimumTonerLevel && !tonerLowNotified) {
                tonerLowNotified = true;
                publishLowResourceEvent("toner", () -> eventPublisher.publishTonerLow(tonerLevel));
            }
            if (paperLevel <= minimumPaperLevel && !paperLowNotified) {
                paperLowNotified = true;
                publishLowResourceEvent("paper", () -> eventPublisher.publishPaperLow(paperLevel));
            }

            return new Ticket(number, type, type.getBasePrice(), Instant.now());
        } finally {
            lock.unlock();
        }
    }

    /**
     * Publishes a resource-low event, recording a metric either way. A broker
     * outage here must not fail the print request that already succeeded.
     */
    private void publishLowResourceEvent(String resource, Runnable publish) {
        try {
            publish.run();
            registry.counter("printing_resource_low_events_total", "resource", resource, "outcome", "published").increment();
        } catch (AmqpException e) {
            log.warn("Could not publish {} low event: {}", resource, e.getMessage());
            registry.counter("printing_resource_low_events_total", "resource", resource, "outcome", "failed").increment();
        }
    }

    /**
     * Refill toner back to full. Returns the new status.
     */
    public MachineStatus refillToner() {
        lock.lock();
        try {
            tonerLevel = fullTonerLevel;
            tonerLowNotified = false;
            return currentStatus();
        } finally {
            lock.unlock();
        }
    }

    /**
     * Add one pack of paper, capped at the full tray size.
     */
    public MachineStatus refillPaper() {
        lock.lock();
        try {
            paperLevel = Math.min(fullPaperTray, paperLevel + sheetsPerPack);
            if (paperLevel > minimumPaperLevel) {
                paperLowNotified = false;
            }
            return currentStatus();
        } finally {
            lock.unlock();
        }
    }

    public MachineStatus getStatus() {
        lock.lock();
        try {
            return currentStatus();
        } finally {
            lock.unlock();
        }
    }

    private MachineStatus currentStatus() {
        return new MachineStatus(
                tonerLevel,
                paperLevel,
                ticketsPrinted.get(),
                tonerLevel <= minimumTonerLevel,
                paperLevel <= minimumPaperLevel);
    }
}
