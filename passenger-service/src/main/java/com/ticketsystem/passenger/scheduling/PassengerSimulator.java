package com.ticketsystem.passenger.scheduling;

import com.ticketsystem.passenger.model.Ticket;
import com.ticketsystem.passenger.model.TicketType;
import com.ticketsystem.passenger.service.PrintingServiceClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;

import java.util.Random;

/**
 * Simulates passengers arriving at random and requesting a ticket -
 * the load-generating equivalent of the passenger threads in the
 * original concurrent-ticket-system project.
 */
@Component
public class PassengerSimulator {

    private static final Logger log = LoggerFactory.getLogger(PassengerSimulator.class);
    private static final TicketType[] TICKET_TYPES = TicketType.values();

    private final PrintingServiceClient client;
    private final Random random = new Random();

    public PassengerSimulator(PrintingServiceClient client) {
        this.client = client;
    }

    @Scheduled(fixedDelayString = "${passenger.request-interval-ms:2000}")
    public void requestTicket() {
        TicketType type = TICKET_TYPES[random.nextInt(TICKET_TYPES.length)];
        try {
            Ticket ticket = client.printTicket(type);
            log.info("Printed ticket #{} ({})", ticket.getTicketNumber(), ticket.getType());
        } catch (HttpClientErrorException.Conflict e) {
            log.warn("Could not print {} ticket - resources unavailable", type);
        } catch (RestClientException e) {
            log.warn("Could not reach printing-service: {}", e.getMessage());
        }
    }
}
