package com.ticketsystem.resupply.messaging;

import com.ticketsystem.resupply.model.MachineStatus;
import com.ticketsystem.resupply.service.PrintingServiceClient;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.ResourceAccessException;

import static org.mockito.Mockito.*;

class ResourceLowEventListenerTest {

    @Test
    void refillsTonerWhenTonerLowEventReceived() {
        PrintingServiceClient client = mock(PrintingServiceClient.class);
        when(client.refillToner()).thenReturn(new MachineStatus());

        new ResourceLowEventListener(client)
                .handle(new ResourceLowEvent(ResourceLowEvent.Resource.TONER, 0));

        verify(client, times(1)).refillToner();
        verify(client, never()).refillPaper();
    }

    @Test
    void refillsPaperWhenPaperLowEventReceived() {
        PrintingServiceClient client = mock(PrintingServiceClient.class);
        when(client.refillPaper()).thenReturn(new MachineStatus());

        new ResourceLowEventListener(client)
                .handle(new ResourceLowEvent(ResourceLowEvent.Resource.PAPER, 5));

        verify(client, times(1)).refillPaper();
        verify(client, never()).refillToner();
    }

    @Test
    void swallowsExceptionWhenPrintingServiceUnreachable() {
        PrintingServiceClient client = mock(PrintingServiceClient.class);
        when(client.refillToner()).thenThrow(new ResourceAccessException("connection refused"));

        // Should not throw
        new ResourceLowEventListener(client)
                .handle(new ResourceLowEvent(ResourceLowEvent.Resource.TONER, 0));

        verify(client, times(1)).refillToner();
    }
}
