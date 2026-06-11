package com.ticketsystem.resupply.model;

/**
 * Mirrors the status payload returned by printing-service's GET /status.
 */
public class MachineStatus {
    private int tonerLevel;
    private int paperLevel;
    private int ticketsPrinted;
    private boolean tonerLow;
    private boolean paperLow;

    public int getTonerLevel() {
        return tonerLevel;
    }

    public void setTonerLevel(int tonerLevel) {
        this.tonerLevel = tonerLevel;
    }

    public int getPaperLevel() {
        return paperLevel;
    }

    public void setPaperLevel(int paperLevel) {
        this.paperLevel = paperLevel;
    }

    public int getTicketsPrinted() {
        return ticketsPrinted;
    }

    public void setTicketsPrinted(int ticketsPrinted) {
        this.ticketsPrinted = ticketsPrinted;
    }

    public boolean isTonerLow() {
        return tonerLow;
    }

    public void setTonerLow(boolean tonerLow) {
        this.tonerLow = tonerLow;
    }

    public boolean isPaperLow() {
        return paperLow;
    }

    public void setPaperLow(boolean paperLow) {
        this.paperLow = paperLow;
    }
}
