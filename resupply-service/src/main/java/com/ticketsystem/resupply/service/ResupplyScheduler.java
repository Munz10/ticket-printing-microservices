package com.ticketsystem.resupply.service;

import com.ticketsystem.resupply.model.MachineStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;

/**
 * Polls printing-service on a fixed interval and triggers refills when
 * toner/paper run low. This is the microservices equivalent of the
 * technician threads in the original concurrent-ticket-system project.
 */
@Component
public class ResupplyScheduler {

    private static final Logger log = LoggerFactory.getLogger(ResupplyScheduler.class);

    private final PrintingServiceClient client;

    public ResupplyScheduler(PrintingServiceClient client) {
        this.client = client;
    }

    @Scheduled(fixedDelayString = "${resupply.poll-interval-ms}")
    public void checkAndRefill() {
        try {
            MachineStatus status = client.getStatus();
            log.info("Status check - toner={}, paper={}, ticketsPrinted={}",
                    status.getTonerLevel(), status.getPaperLevel(), status.getTicketsPrinted());

            if (status.isTonerLow()) {
                log.info("Toner low ({}). Requesting refill...", status.getTonerLevel());
                MachineStatus updated = client.refillToner();
                log.info("Toner refilled. New level: {}", updated.getTonerLevel());
            }

            if (status.isPaperLow()) {
                log.info("Paper low ({}). Requesting refill...", status.getPaperLevel());
                MachineStatus updated = client.refillPaper();
                log.info("Paper refilled. New level: {}", updated.getPaperLevel());
            }
        } catch (RestClientException e) {
            log.warn("Could not reach printing-service: {}", e.getMessage());
        }
    }
}
