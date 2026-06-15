package com.ticketsystem.resupply.messaging;

import com.ticketsystem.resupply.model.MachineStatus;
import com.ticketsystem.resupply.service.PrintingServiceClient;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.ResourceAccessException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

class ResourceLowEventListenerTest {

    private SimpleMeterRegistry registry;

    @BeforeEach
    void setUp() {
        registry = new SimpleMeterRegistry();
    }

    @Test
    void refillsTonerWhenTonerLowEventReceived() {
        PrintingServiceClient client = mock(PrintingServiceClient.class);
        when(client.refillToner()).thenReturn(new MachineStatus());

        new ResourceLowEventListener(client, registry)
                .handle(new ResourceLowEvent(ResourceLowEvent.Resource.TONER, 0));

        verify(client, times(1)).refillToner();
        verify(client, never()).refillPaper();
        assertEquals(1.0, registry.counter("resupply_refills_total", "resource", "toner", "outcome", "success").count());
    }

    @Test
    void refillsPaperWhenPaperLowEventReceived() {
        PrintingServiceClient client = mock(PrintingServiceClient.class);
        when(client.refillPaper()).thenReturn(new MachineStatus());

        new ResourceLowEventListener(client, registry)
                .handle(new ResourceLowEvent(ResourceLowEvent.Resource.PAPER, 5));

        verify(client, times(1)).refillPaper();
        verify(client, never()).refillToner();
        assertEquals(1.0, registry.counter("resupply_refills_total", "resource", "paper", "outcome", "success").count());
    }

    @Test
    void swallowsExceptionWhenPrintingServiceUnreachable() {
        PrintingServiceClient client = mock(PrintingServiceClient.class);
        when(client.refillToner()).thenThrow(new ResourceAccessException("connection refused"));

        // Should not throw
        new ResourceLowEventListener(client, registry)
                .handle(new ResourceLowEvent(ResourceLowEvent.Resource.TONER, 0));

        verify(client, times(1)).refillToner();
        assertEquals(1.0, registry.counter("resupply_refills_total", "resource", "toner", "outcome", "failed").count());
    }
}
