package com.ticketsystem.printing.model;

/**
 * Snapshot of the printing machine's current resource levels.
 */
public class MachineStatus {
    private final int tonerLevel;
    private final int paperLevel;
    private final int ticketsPrinted;
    private final boolean tonerLow;
    private final boolean paperLow;

    public MachineStatus(int tonerLevel, int paperLevel, int ticketsPrinted, boolean tonerLow, boolean paperLow) {
        this.tonerLevel = tonerLevel;
        this.paperLevel = paperLevel;
        this.ticketsPrinted = ticketsPrinted;
        this.tonerLow = tonerLow;
        this.paperLow = paperLow;
    }

    public int getTonerLevel() {
        return tonerLevel;
    }

    public int getPaperLevel() {
        return paperLevel;
    }

    public int getTicketsPrinted() {
        return ticketsPrinted;
    }

    public boolean isTonerLow() {
        return tonerLow;
    }

    public boolean isPaperLow() {
        return paperLow;
    }
}
