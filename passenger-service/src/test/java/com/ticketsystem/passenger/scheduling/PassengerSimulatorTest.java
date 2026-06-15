package com.ticketsystem.passenger.scheduling;

import com.ticketsystem.passenger.model.Ticket;
import com.ticketsystem.passenger.model.TicketType;
import com.ticketsystem.passenger.service.PrintingServiceClient;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class PassengerSimulatorTest {

    private SimpleMeterRegistry registry;

    @BeforeEach
    void setUp() {
        registry = new SimpleMeterRegistry();
    }

    @Test
    void printsATicketOfSomeType() {
        PrintingServiceClient client = mock(PrintingServiceClient.class);
        when(client.printTicket(any(TicketType.class))).thenReturn(new Ticket());

        new PassengerSimulator(client, registry).requestTicket();

        verify(client, times(1)).printTicket(any(TicketType.class));
        assertSingleCounterWithOutcome("printed");
    }

    @Test
    void swallowsConflictWhenResourcesUnavailable() {
        PrintingServiceClient client = mock(PrintingServiceClient.class);
        when(client.printTicket(any(TicketType.class)))
                .thenThrow(HttpClientErrorException.create(HttpStatus.CONFLICT, "Conflict", null, null, null));

        // Should not throw
        new PassengerSimulator(client, registry).requestTicket();

        verify(client, times(1)).printTicket(any(TicketType.class));
        assertSingleCounterWithOutcome("rejected");
    }

    @Test
    void swallowsExceptionWhenPrintingServiceUnreachable() {
        PrintingServiceClient client = mock(PrintingServiceClient.class);
        when(client.printTicket(any(TicketType.class)))
                .thenThrow(new ResourceAccessException("connection refused"));

        // Should not throw
        new PassengerSimulator(client, registry).requestTicket();

        verify(client, times(1)).printTicket(any(TicketType.class));
        assertSingleCounterWithOutcome("error");
    }

    private void assertSingleCounterWithOutcome(String outcome) {
        double total = registry.find("passenger_tickets_requested_total")
                .tag("outcome", outcome)
                .counters()
                .stream()
                .mapToDouble(io.micrometer.core.instrument.Counter::count)
                .sum();
        org.junit.jupiter.api.Assertions.assertEquals(1.0, total);
    }
}
