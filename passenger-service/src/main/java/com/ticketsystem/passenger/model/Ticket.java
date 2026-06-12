package com.ticketsystem.passenger.model;

import java.time.Instant;

/**
 * Mirrors the ticket payload returned by printing-service's POST /tickets.
 */
public class Ticket {
    private int ticketNumber;
    private TicketType type;
    private double price;
    private Instant printedAt;

    public int getTicketNumber() {
        return ticketNumber;
    }

    public void setTicketNumber(int ticketNumber) {
        this.ticketNumber = ticketNumber;
    }

    public TicketType getType() {
        return type;
    }

    public void setType(TicketType type) {
        this.type = type;
    }

    public double getPrice() {
        return price;
    }

    public void setPrice(double price) {
        this.price = price;
    }

    public Instant getPrintedAt() {
        return printedAt;
    }

    public void setPrintedAt(Instant printedAt) {
        this.printedAt = printedAt;
    }
}
