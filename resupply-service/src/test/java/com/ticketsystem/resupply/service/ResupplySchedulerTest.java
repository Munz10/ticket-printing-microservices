package com.ticketsystem.resupply.service;

import com.ticketsystem.resupply.model.MachineStatus;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.*;

class ResupplySchedulerTest {

    @Test
    void refillsTonerAndPaperWhenBothLow() {
        PrintingServiceClient client = mock(PrintingServiceClient.class);

        MachineStatus low = new MachineStatus();
        low.setTonerLevel(5);
        low.setPaperLevel(5);
        low.setTonerLow(true);
        low.setPaperLow(true);

        when(client.getStatus()).thenReturn(low);
        when(client.refillToner()).thenReturn(new MachineStatus());
        when(client.refillPaper()).thenReturn(new MachineStatus());

        new ResupplyScheduler(client).checkAndRefill();

        verify(client, times(1)).refillToner();
        verify(client, times(1)).refillPaper();
    }

    @Test
    void doesNothingWhenResourcesAreSufficient() {
        PrintingServiceClient client = mock(PrintingServiceClient.class);

        MachineStatus ok = new MachineStatus();
        ok.setTonerLevel(60);
        ok.setPaperLevel(100);
        ok.setTonerLow(false);
        ok.setPaperLow(false);

        when(client.getStatus()).thenReturn(ok);

        new ResupplyScheduler(client).checkAndRefill();

        verify(client, never()).refillToner();
        verify(client, never()).refillPaper();
    }

    @Test
    void swallowsExceptionWhenPrintingServiceUnreachable() {
        PrintingServiceClient client = mock(PrintingServiceClient.class);
        when(client.getStatus()).thenThrow(new org.springframework.web.client.ResourceAccessException("connection refused"));

        // Should not throw
        new ResupplyScheduler(client).checkAndRefill();

        verify(client, never()).refillToner();
        verify(client, never()).refillPaper();
    }
}
