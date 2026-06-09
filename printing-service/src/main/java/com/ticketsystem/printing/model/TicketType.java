package com.ticketsystem.printing.model;

/**
 * Ticket types with different resource (toner/paper) costs and base prices.
 * Mirrors the design from the original concurrent-ticket-system project.
 */
public enum TicketType {
    ECONOMY("Economy Class", 5, 1, 50.0),
    BUSINESS("Business Class", 10, 2, 150.0),
    FIRST_CLASS("First Class", 15, 3, 300.0),
    VIP_PREMIUM("VIP Premium", 20, 4, 500.0);

    private final String displayName;
    private final int tonerCost;
    private final int paperCost;
    private final double basePrice;

    TicketType(String displayName, int tonerCost, int paperCost, double basePrice) {
        this.displayName = displayName;
        this.tonerCost = tonerCost;
        this.paperCost = paperCost;
        this.basePrice = basePrice;
    }

    public String getDisplayName() {
        return displayName;
    }

    public int getTonerCost() {
        return tonerCost;
    }

    public int getPaperCost() {
        return paperCost;
    }

    public double getBasePrice() {
        return basePrice;
    }

    public boolean canPrint(int availableToner, int availablePaper) {
        return availableToner >= tonerCost && availablePaper >= paperCost;
    }
}
