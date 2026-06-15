package com.ticketsystem.printing.controller;

import com.ticketsystem.printing.exception.ResourceUnavailableException;
import com.ticketsystem.printing.model.MachineStatus;
import com.ticketsystem.printing.model.Ticket;
import com.ticketsystem.printing.model.TicketType;
import com.ticketsystem.printing.service.PrintingMachineService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(PrintingController.class)
class PrintingControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private PrintingMachineService machine;

    @Test
    void printTicketReturnsTicket() throws Exception {
        when(machine.printTicket(TicketType.BUSINESS))
                .thenReturn(new Ticket(1, TicketType.BUSINESS, TicketType.BUSINESS.getBasePrice(), Instant.now()));

        mockMvc.perform(post("/tickets").param("type", "BUSINESS"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ticketNumber").value(1))
                .andExpect(jsonPath("$.type").value("BUSINESS"));
    }

    @Test
    void printTicketDefaultsToEconomy() throws Exception {
        when(machine.printTicket(TicketType.ECONOMY))
                .thenReturn(new Ticket(1, TicketType.ECONOMY, TicketType.ECONOMY.getBasePrice(), Instant.now()));

        mockMvc.perform(post("/tickets"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.type").value("ECONOMY"));
    }

    @Test
    void printTicketWithInvalidTypeReturns400() throws Exception {
        mockMvc.perform(post("/tickets").param("type", "NOT_A_TYPE"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("ECONOMY")));
    }

    @Test
    void printTicketReturns409WhenResourcesUnavailable() throws Exception {
        when(machine.printTicket(any(TicketType.class)))
                .thenThrow(new ResourceUnavailableException("not enough toner"));

        mockMvc.perform(post("/tickets").param("type", "VIP_PREMIUM"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.error").value("Resource Unavailable"))
                .andExpect(jsonPath("$.message").value("not enough toner"));
    }

    @Test
    void getStatusReturnsMachineStatus() throws Exception {
        when(machine.getStatus()).thenReturn(new MachineStatus(60, 100, 5, false, false));

        mockMvc.perform(get("/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tonerLevel").value(60))
                .andExpect(jsonPath("$.paperLevel").value(100))
                .andExpect(jsonPath("$.ticketsPrinted").value(5));
    }

    @Test
    void refillTonerReturnsUpdatedStatus() throws Exception {
        when(machine.refillToner()).thenReturn(new MachineStatus(100, 100, 5, false, false));

        mockMvc.perform(post("/refill/toner"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tonerLevel").value(100));
    }

    @Test
    void refillPaperReturnsUpdatedStatus() throws Exception {
        when(machine.refillPaper()).thenReturn(new MachineStatus(60, 150, 5, false, false));

        mockMvc.perform(post("/refill/paper"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paperLevel").value(150));
    }
}
