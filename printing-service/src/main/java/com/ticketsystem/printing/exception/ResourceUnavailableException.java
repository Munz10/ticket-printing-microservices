package com.ticketsystem.printing.exception;

/**
 * Thrown when the machine does not have enough toner/paper to print
 * the requested ticket type.
 */
public class ResourceUnavailableException extends RuntimeException {
    public ResourceUnavailableException(String message) {
        super(message);
    }
}
