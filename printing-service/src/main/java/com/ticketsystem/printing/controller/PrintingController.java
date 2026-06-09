package com.ticketsystem.printing.controller;

import com.ticketsystem.printing.model.MachineStatus;
import com.ticketsystem.printing.model.Ticket;
import com.ticketsystem.printing.model.TicketType;
import com.ticketsystem.printing.service.PrintingMachineService;
import org.springframework.web.bind.annotation.*;

@RestController
public class PrintingController {

    private final PrintingMachineService machine;

    public PrintingController(PrintingMachineService machine) {
        this.machine = machine;
    }

    /**
     * Print a ticket. Defaults to ECONOMY if no type is given.
     * Example: POST /tickets?type=BUSINESS
     */
    @PostMapping("/tickets")
    public Ticket printTicket(@RequestParam(name = "type", defaultValue = "ECONOMY") TicketType type) {
        return machine.printTicket(type);
    }

    /**
     * Current toner/paper levels and ticket count.
     */
    @GetMapping("/status")
    public MachineStatus getStatus() {
        return machine.getStatus();
    }

    /**
     * Refill toner to full. Called by the resupply-service.
     */
    @PostMapping("/refill/toner")
    public MachineStatus refillToner() {
        return machine.refillToner();
    }

    /**
     * Add a pack of paper. Called by the resupply-service.
     */
    @PostMapping("/refill/paper")
    public MachineStatus refillPaper() {
        return machine.refillPaper();
    }
}
