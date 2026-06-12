package com.ticketsystem.resupply.messaging;

/**
 * Mirrors printing-service's ResourceLowEvent. Published when toner or
 * paper drops to/below its minimum threshold.
 */
public class ResourceLowEvent {

    public enum Resource { TONER, PAPER }

    private Resource resource;
    private int currentLevel;

    public ResourceLowEvent() {
    }

    public ResourceLowEvent(Resource resource, int currentLevel) {
        this.resource = resource;
        this.currentLevel = currentLevel;
    }

    public Resource getResource() {
        return resource;
    }

    public void setResource(Resource resource) {
        this.resource = resource;
    }

    public int getCurrentLevel() {
        return currentLevel;
    }

    public void setCurrentLevel(int currentLevel) {
        this.currentLevel = currentLevel;
    }
}
