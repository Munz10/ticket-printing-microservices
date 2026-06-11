package com.ticketsystem.resupply.service;

import com.ticketsystem.resupply.model.MachineStatus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

/**
 * Thin HTTP client for printing-service's status and refill endpoints.
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

    public MachineStatus getStatus() {
        return restTemplate.getForObject(baseUrl + "/status", MachineStatus.class);
    }

    public MachineStatus refillToner() {
        return restTemplate.postForObject(baseUrl + "/refill/toner", null, MachineStatus.class);
    }

    public MachineStatus refillPaper() {
        return restTemplate.postForObject(baseUrl + "/refill/paper", null, MachineStatus.class);
    }
}
