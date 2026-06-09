package com.ticketsystem.printing.service;

import com.ticketsystem.printing.exception.ResourceUnavailableException;
import com.ticketsystem.printing.model.MachineStatus;
import com.ticketsystem.printing.model.Ticket;
import com.ticketsystem.printing.model.TicketType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PrintingMachineServiceTest {

    private PrintingMachineService machine;

    @BeforeEach
    void setUp() {
        // toner=60, paper=100, full toner=100, full paper=250, min toner=10, min paper=10, pack=50
        machine = new PrintingMachineService(60, 100, 100, 250, 10, 10, 50);
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
}
