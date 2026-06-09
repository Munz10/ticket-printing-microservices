package com.ticketsystem.printing.model;

import java.time.Instant;

/**
 * A printed ticket, returned to clients of the printing-service.
 */
public class Ticket {
    private final int ticketNumber;
    private final TicketType type;
    private final double price;
    private final Instant printedAt;

    public Ticket(int ticketNumber, TicketType type, double price, Instant printedAt) {
        this.ticketNumber = ticketNumber;
        this.type = type;
        this.price = price;
        this.printedAt = printedAt;
    }

    public int getTicketNumber() {
        return ticketNumber;
    }

    public TicketType getType() {
        return type;
    }

    public double getPrice() {
        return price;
    }

    public Instant getPrintedAt() {
        return printedAt;
    }
}
