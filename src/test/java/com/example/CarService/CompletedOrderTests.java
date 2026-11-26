package com.example.CarService;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * A completed order is locked: nothing about its work items or parts may change, and it cannot be completed twice.
 * Each test starts from its own freshly completed order with one finished work item and one part.
 */
@SpringBootTest
@AutoConfigureMockMvc
class CompletedOrderTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private long orderId;
    private long workItemId;
    private long partId;

    @BeforeEach
    void createCompletedOrder() throws Exception {
        String vehicle = "{\"licensePlate\":\"C-" + UUID.randomUUID().toString().substring(0, 8)
                + "\",\"ownerName\":\"Test Owner\",\"make\":\"Skoda\",\"model\":\"Octavia\",\"year\":2022}";
        long vehicleId = idOf(mockMvc.perform(post("/vehicles").with(jwt())
                .contentType(MediaType.APPLICATION_JSON).content(vehicle)).andReturn().getResponse().getContentAsString());
        orderId = idOf(mockMvc.perform(post("/vehicles/" + vehicleId + "/orders").with(jwt()))
                .andReturn().getResponse().getContentAsString());

        workItemId = idOf(mockMvc.perform(post("/orders/" + orderId + "/work-items").with(jwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"description\":\"Brake check\",\"estimatedHours\":1,\"mechanicName\":\"Bogdan\"}"))
                .andReturn().getResponse().getContentAsString());
        partId = idOf(mockMvc.perform(post("/orders/" + orderId + "/parts").with(jwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Brake fluid\",\"quantity\":1,\"unitPrice\":20}"))
                .andReturn().getResponse().getContentAsString());

        mockMvc.perform(patch("/work-items/" + workItemId + "/finish").with(jwt())).andExpect(status().isOk());
        mockMvc.perform(patch("/orders/" + orderId + "/complete").with(jwt())).andExpect(status().isOk());
    }

    @Test
    void cannotCompleteTwice() throws Exception {
        mockMvc.perform(patch("/orders/" + orderId + "/complete").with(jwt()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("Order is already completed"));
    }

    @Test
    void cannotAddWorkItem() throws Exception {
        mockMvc.perform(post("/orders/" + orderId + "/work-items").with(jwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"description\":\"Late extra\",\"estimatedHours\":1,\"mechanicName\":\"Stefan\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("Cannot add work items to a completed order"));
    }

    @Test
    void cannotChangeOrDeleteWorkItem() throws Exception {
        mockMvc.perform(patch("/work-items/" + workItemId + "/finish").with(jwt()))
                .andExpect(status().isConflict());
        mockMvc.perform(delete("/work-items/" + workItemId).with(jwt()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("Cannot delete work items of a completed order"));
    }

    @Test
    void cannotAddOrDeletePart() throws Exception {
        mockMvc.perform(post("/orders/" + orderId + "/parts").with(jwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Wiper\",\"quantity\":1,\"unitPrice\":15}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("Cannot add parts to a completed order"));
        mockMvc.perform(delete("/parts/" + partId).with(jwt()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("Cannot delete parts of a completed order"));
    }

    @Test
    void completedOrderStaysReadable() throws Exception {
        mockMvc.perform(get("/orders/" + orderId).with(jwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"));
        mockMvc.perform(get("/orders/" + orderId + "/work-items").with(jwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].status").value("DONE"));
    }

    private long idOf(String json) throws Exception {
        return objectMapper.readTree(json).get("id").asLong();
    }
}
