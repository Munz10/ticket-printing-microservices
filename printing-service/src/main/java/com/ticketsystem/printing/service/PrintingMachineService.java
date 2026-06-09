package com.ticketsystem.printing.service;

import com.ticketsystem.printing.exception.ResourceUnavailableException;
import com.ticketsystem.printing.model.MachineStatus;
import com.ticketsystem.printing.model.Ticket;
import com.ticketsystem.printing.model.TicketType;
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
 * (REST) model instead of blocking threads.
 */
@Service
public class PrintingMachineService {

    private final ReentrantLock lock = new ReentrantLock();
    private final AtomicInteger ticketsPrinted = new AtomicInteger(0);

    private int tonerLevel;
    private int paperLevel;

    private final int fullTonerLevel;
    private final int fullPaperTray;
    private final int minimumTonerLevel;
    private final int minimumPaperLevel;
    private final int sheetsPerPack;

    public PrintingMachineService(
            @Value("${machine.toner.initial}") int initialToner,
            @Value("${machine.paper.initial}") int initialPaper,
            @Value("${machine.toner.full}") int fullTonerLevel,
            @Value("${machine.paper.full}") int fullPaperTray,
            @Value("${machine.toner.minimum}") int minimumTonerLevel,
            @Value("${machine.paper.minimum}") int minimumPaperLevel,
            @Value("${machine.paper.sheets-per-pack}") int sheetsPerPack) {
        this.tonerLevel = initialToner;
        this.paperLevel = initialPaper;
        this.fullTonerLevel = fullTonerLevel;
        this.fullPaperTray = fullPaperTray;
        this.minimumTonerLevel = minimumTonerLevel;
        this.minimumPaperLevel = minimumPaperLevel;
        this.sheetsPerPack = sheetsPerPack;
    }

    /**
     * Print a ticket of the given type. Throws ResourceUnavailableException
     * (mapped to HTTP 409) if there isn't enough toner/paper.
     */
    public Ticket printTicket(TicketType type) {
        lock.lock();
        try {
            if (!type.canPrint(tonerLevel, paperLevel)) {
                throw new ResourceUnavailableException(String.format(
                        "Cannot print %s ticket - need toner=%d, paper=%d but have toner=%d, paper=%d",
                        type.getDisplayName(), type.getTonerCost(), type.getPaperCost(), tonerLevel, paperLevel));
            }

            tonerLevel -= type.getTonerCost();
            paperLevel -= type.getPaperCost();
            int number = ticketsPrinted.incrementAndGet();

            return new Ticket(number, type, type.getBasePrice(), Instant.now());
        } finally {
            lock.unlock();
        }
    }

    /**
     * Refill toner back to full. Returns the new status.
     */
    public MachineStatus refillToner() {
        lock.lock();
        try {
            tonerLevel = fullTonerLevel;
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
