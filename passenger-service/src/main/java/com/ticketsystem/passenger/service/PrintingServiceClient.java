package com.ticketsystem.passenger.service;

import com.ticketsystem.passenger.model.Ticket;
import com.ticketsystem.passenger.model.TicketType;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * Thin HTTP client for printing-service's ticket endpoint.
 */
@Component
public class PrintingServiceClient {

    private final RestTemplate restTemplate;
    private final String baseUrl;

    public PrintingServiceClient(RestTemplate restTemplate,
                                  @Value("${printing-service.base-url}") String baseUrl) {
        this.restTemplate = restTemplate;
        this.baseUrl = baseUrl;
    }

    public Ticket printTicket(TicketType type) {
        String url = UriComponentsBuilder.fromHttpUrl(baseUrl + "/tickets")
                .queryParam("type", type)
                .toUriString();
        return restTemplate.postForObject(url, null, Ticket.class);
    }
}
