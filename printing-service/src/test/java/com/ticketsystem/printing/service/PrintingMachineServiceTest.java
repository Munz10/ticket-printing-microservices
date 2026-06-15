package com.ticketsystem.printing.service;

import com.ticketsystem.printing.exception.ResourceUnavailableException;
import com.ticketsystem.printing.messaging.ResourceEventPublisher;
import com.ticketsystem.printing.model.MachineStatus;
import com.ticketsystem.printing.model.Ticket;
import com.ticketsystem.printing.model.TicketType;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.AmqpException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class PrintingMachineServiceTest {

    private PrintingMachineService machine;
    private ResourceEventPublisher eventPublisher;
    private SimpleMeterRegistry registry;

    @BeforeEach
    void setUp() {
        eventPublisher = mock(ResourceEventPublisher.class);
        registry = new SimpleMeterRegistry();
        // toner=60, paper=100, full toner=100, full paper=250, min toner=10, min paper=10, pack=50
        machine = new PrintingMachineService(eventPublisher, registry, 60, 100, 100, 250, 10, 10, 50);
    }

    @Test
    void printsTicketAndConsumesResources() {
        Ticket ticket = machine.printTicket(TicketType.ECONOMY);

        assertEquals(1, ticket.getTicketNumber());
        assertEquals(TicketType.ECONOMY, ticket.getType());

        MachineStatus status = machine.getStatus();
        assertEquals(60 - TicketType.ECONOMY.getTonerCost(), status.getTonerLevel());
        assertEquals(100 - TicketType.ECONOMY.getPaperCost(), status.getPaperLevel());
        assertEquals(1, status.getTicketsPrinted());
    }

    @Test
    void throwsWhenResourcesInsufficient() {
        // Drain toner: 60 / 20 (VIP_PREMIUM cost) = 3 prints possible
        for (int i = 0; i < 3; i++) {
            machine.printTicket(TicketType.VIP_PREMIUM);
        }

        assertThrows(ResourceUnavailableException.class,
                () -> machine.printTicket(TicketType.VIP_PREMIUM));
    }

    @Test
    void refillTonerRestoresToFull() {
        machine.printTicket(TicketType.VIP_PREMIUM);
        machine.refillToner();

        assertEquals(100, machine.getStatus().getTonerLevel());
    }

    @Test
    void refillPaperAddsOnePackCappedAtFullTray() {
        MachineStatus status = machine.refillPaper();
        assertEquals(150, status.getPaperLevel());

        // Refill repeatedly, should cap at 250
        machine.refillPaper();
        machine.refillPaper();
        status = machine.refillPaper();
        assertEquals(250, status.getPaperLevel());
    }

    @Test
    void publishesTonerLowEventOnceWhenThresholdCrossed() {
        // toner=60, min=10, VIP_PREMIUM costs 20 -> after 3 prints toner=0 (crosses threshold once)
        machine.printTicket(TicketType.VIP_PREMIUM);
        machine.printTicket(TicketType.VIP_PREMIUM);
        verify(eventPublisher, never()).publishTonerLow(anyInt());

        machine.printTicket(TicketType.VIP_PREMIUM);
        verify(eventPublisher, times(1)).publishTonerLow(0);
    }

    @Test
    void resetsTonerLowNotificationAfterRefill() {
        for (int i = 0; i < 3; i++) {
            machine.printTicket(TicketType.VIP_PREMIUM);
        }
        verify(eventPublisher, times(1)).publishTonerLow(anyInt());

        machine.refillToner();

        // Drain again from full (100) - takes 5 prints at 20 toner each to reach 0
        for (int i = 0; i < 5; i++) {
            machine.printTicket(TicketType.VIP_PREMIUM);
        }
        verify(eventPublisher, times(2)).publishTonerLow(anyInt());
    }

    @Test
    void recordsMetricsForPrintedTicketsAndLevels() {
        machine.printTicket(TicketType.ECONOMY);

        assertEquals(1.0, registry.counter("printing_tickets_printed_total", "type", "ECONOMY").count());
        assertEquals(60 - TicketType.ECONOMY.getTonerCost(), registry.get("printing_toner_level").gauge().value());
        assertEquals(100 - TicketType.ECONOMY.getPaperCost(), registry.get("printing_paper_level").gauge().value());
    }

    @Test
    void recordsResourceUnavailableMetric() {
        for (int i = 0; i < 3; i++) {
            machine.printTicket(TicketType.VIP_PREMIUM);
        }

        assertThrows(ResourceUnavailableException.class, () -> machine.printTicket(TicketType.VIP_PREMIUM));
        assertEquals(1.0, registry.counter("printing_resource_unavailable_total").count());
    }

    @Test
    void printSucceedsAndRecordsFailureMetricWhenEventPublishThrows() {
        doThrow(mock(AmqpException.class)).when(eventPublisher).publishTonerLow(anyInt());

        for (int i = 0; i < 3; i++) {
            machine.printTicket(TicketType.VIP_PREMIUM);
        }

        assertEquals(0, machine.getStatus().getTonerLevel());
        assertEquals(1.0, registry.counter("printing_resource_low_events_total", "resource", "toner", "outcome", "failed").count());
    }
}
