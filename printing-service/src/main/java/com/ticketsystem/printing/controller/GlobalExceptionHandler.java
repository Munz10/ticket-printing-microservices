package com.ticketsystem.printing.controller;

import com.ticketsystem.printing.exception.ResourceUnavailableException;
import com.ticketsystem.printing.model.TicketType;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.time.Instant;
import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceUnavailableException.class)
    public ResponseEntity<Map<String, Object>> handleResourceUnavailable(ResourceUnavailableException ex) {
        Map<String, Object> body = Map.of(
                "timestamp", Instant.now().toString(),
                "status", HttpStatus.CONFLICT.value(),
                "error", "Resource Unavailable",
                "message", ex.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(body);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<Map<String, Object>> handleInvalidParameter(MethodArgumentTypeMismatchException ex) {
        String message;
        if (ex.getRequiredType() == TicketType.class) {
            String allowed = Arrays.stream(TicketType.values())
                    .map(Enum::name)
                    .collect(Collectors.joining(", "));
            message = String.format("Invalid value '%s' for parameter '%s'. Allowed values: %s",
                    ex.getValue(), ex.getName(), allowed);
        } else {
            message = String.format("Invalid value '%s' for parameter '%s'", ex.getValue(), ex.getName());
        }

        Map<String, Object> body = Map.of(
                "timestamp", Instant.now().toString(),
                "status", HttpStatus.BAD_REQUEST.value(),
                "error", "Bad Request",
                "message", message);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }
}
