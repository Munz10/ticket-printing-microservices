package com.ticketsystem.passenger.scheduling;

import com.ticketsystem.passenger.model.Ticket;
import com.ticketsystem.passenger.model.TicketType;
import com.ticketsystem.passenger.service.PrintingServiceClient;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class PassengerSimulatorTest {

    @Test
    void printsATicketOfSomeType() {
        PrintingServiceClient client = mock(PrintingServiceClient.class);
        when(client.printTicket(any(TicketType.class))).thenReturn(new Ticket());

        new PassengerSimulator(client).requestTicket();

        verify(client, times(1)).printTicket(any(TicketType.class));
    }

    @Test
    void swallowsConflictWhenResourcesUnavailable() {
        PrintingServiceClient client = mock(PrintingServiceClient.class);
        when(client.printTicket(any(TicketType.class)))
                .thenThrow(HttpClientErrorException.create(HttpStatus.CONFLICT, "Conflict", null, null, null));

        // Should not throw
        new PassengerSimulator(client).requestTicket();

        verify(client, times(1)).printTicket(any(TicketType.class));
    }

    @Test
    void swallowsExceptionWhenPrintingServiceUnreachable() {
        PrintingServiceClient client = mock(PrintingServiceClient.class);
        when(client.printTicket(any(TicketType.class)))
                .thenThrow(new ResourceAccessException("connection refused"));

        // Should not throw
        new PassengerSimulator(client).requestTicket();

        verify(client, times(1)).printTicket(any(TicketType.class));
    }
}
