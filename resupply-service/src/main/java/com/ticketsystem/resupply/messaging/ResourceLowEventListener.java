package com.ticketsystem.resupply.messaging;

import com.ticketsystem.resupply.model.MachineStatus;
import com.ticketsystem.resupply.service.PrintingServiceClient;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;

/**
 * Reacts to ResourceLowEvent messages published by printing-service and
 * triggers the corresponding refill. This is the event-driven equivalent
 * of the technician threads in the original concurrent-ticket-system
 * project - instead of polling, resupply-service is notified immediately
 * when toner or paper runs low.
 */
@Component
public class ResourceLowEventListener {

    private static final Logger log = LoggerFactory.getLogger(ResourceLowEventListener.class);

    private final PrintingServiceClient client;
    private final MeterRegistry registry;

    public ResourceLowEventListener(PrintingServiceClient client, MeterRegistry registry) {
        this.client = client;
        this.registry = registry;
    }

    @RabbitListener(queues = RabbitMQConfig.RESOURCE_EVENTS_QUEUE)
    public void handle(ResourceLowEvent event) {
        log.info("Received {} low event (level={})", event.getResource(), event.getCurrentLevel());
        String resource = event.getResource().name().toLowerCase();

        try {
            MachineStatus updated;
            switch (event.getResource()) {
                case TONER -> {
                    updated = client.refillToner();
                    log.info("Toner refilled. New level: {}", updated.getTonerLevel());
                }
                case PAPER -> {
                    updated = client.refillPaper();
                    log.info("Paper refilled. New level: {}", updated.getPaperLevel());
                }
                default -> throw new IllegalStateException("Unexpected resource: " + event.getResource());
            }
            registry.counter("resupply_refills_total", "resource", resource, "outcome", "success").increment();
        } catch (RestClientException e) {
            log.warn("Could not reach printing-service to refill {}: {}", event.getResource(), e.getMessage());
            registry.counter("resupply_refills_total", "resource", resource, "outcome", "failed").increment();
        }
    }
}
