package com.example.CarService;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Deleted records (deletedAt set) must behave as if they do not exist.
 * Each test creates its own data through the API, so tests do not depend on each other.
 */
@SpringBootTest
@AutoConfigureMockMvc
class SoftDeleteTests {

    private static final String KEY_HEADER = "X-API-Key";
    private static final String KEY = "test-key";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void deletedVehicleIsNotFound() throws Exception {
        long vehicleId = createVehicle(uniquePlate());
        mockMvc.perform(delete("/vehicles/" + vehicleId).header(KEY_HEADER, KEY)).andExpect(status().isNoContent());

        // deleting twice, listing its orders, or opening a new order all behave as "not found"
        mockMvc.perform(delete("/vehicles/" + vehicleId).header(KEY_HEADER, KEY)).andExpect(status().isNotFound());
        mockMvc.perform(get("/vehicles/" + vehicleId + "/orders").header(KEY_HEADER, KEY)).andExpect(status().isNotFound());
        mockMvc.perform(post("/vehicles/" + vehicleId + "/orders").header(KEY_HEADER, KEY)).andExpect(status().isNotFound());
    }

    @Test
    void deletedOrderIsNotFound() throws Exception {
        long orderId = createOrder(createVehicle(uniquePlate()));
        mockMvc.perform(delete("/orders/" + orderId).header(KEY_HEADER, KEY)).andExpect(status().isNoContent());

        mockMvc.perform(get("/orders/" + orderId).header(KEY_HEADER, KEY)).andExpect(status().isNotFound());
        mockMvc.perform(patch("/orders/" + orderId + "/complete").header(KEY_HEADER, KEY)).andExpect(status().isNotFound());
        mockMvc.perform(get("/orders/" + orderId + "/work-items").header(KEY_HEADER, KEY)).andExpect(status().isNotFound());
        mockMvc.perform(post("/orders/" + orderId + "/parts").header(KEY_HEADER, KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Filter\",\"quantity\":1,\"unitPrice\":10}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void deletedWorkItemDoesNotBlockCompletion() throws Exception {
        long orderId = createOrder(createVehicle(uniquePlate()));
        long workItemId = createWorkItem(orderId); // new work items start as TODO, which blocks completion

        mockMvc.perform(patch("/orders/" + orderId + "/complete").header(KEY_HEADER, KEY)).andExpect(status().isConflict());

        mockMvc.perform(delete("/work-items/" + workItemId).header(KEY_HEADER, KEY)).andExpect(status().isNoContent());
        mockMvc.perform(patch("/work-items/" + workItemId + "/finish").header(KEY_HEADER, KEY)).andExpect(status().isNotFound());

        mockMvc.perform(patch("/orders/" + orderId + "/complete").header(KEY_HEADER, KEY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"));
    }

    @Test
    void deletedPartsAndOrdersDoNotCountInRevenue() throws Exception {
        long orderId = createOrder(createVehicle(uniquePlate()));
        addPart(orderId, 2, 50.0);                    // 100.00, kept
        long removedPartId = addPart(orderId, 1, 30.0); // 30.00, deleted before completion
        mockMvc.perform(delete("/parts/" + removedPartId).header(KEY_HEADER, KEY)).andExpect(status().isNoContent());

        double before = revenueThisMonth();
        mockMvc.perform(patch("/orders/" + orderId + "/complete").header(KEY_HEADER, KEY)).andExpect(status().isOk());
        assertThat(revenueThisMonth() - before).isEqualTo(100.0); // only the part that was not deleted

        mockMvc.perform(delete("/orders/" + orderId).header(KEY_HEADER, KEY)).andExpect(status().isNoContent());
        assertThat(revenueThisMonth()).isEqualTo(before); // a deleted order no longer counts
    }

    @Test
    void plateOfDeletedVehicleCanBeUsedAgain() throws Exception {
        String plate = uniquePlate();
        long vehicleId = createVehicle(plate);

        postVehicle(plate).andExpect(status().isConflict()); // still active: duplicate

        mockMvc.perform(delete("/vehicles/" + vehicleId).header(KEY_HEADER, KEY)).andExpect(status().isNoContent());
        postVehicle(plate).andExpect(status().isCreated()); // deleted: plate is free again
    }

    // ---- helpers ----

    private String uniquePlate() {
        return "T-" + UUID.randomUUID().toString().substring(0, 8);
    }

    private org.springframework.test.web.servlet.ResultActions postVehicle(String plate) throws Exception {
        String body = "{\"licensePlate\":\"" + plate + "\",\"ownerName\":\"Test Owner\",\"make\":\"VW\",\"model\":\"Golf\",\"year\":2021}";
        return mockMvc.perform(post("/vehicles").header(KEY_HEADER, KEY).contentType(MediaType.APPLICATION_JSON).content(body));
    }

    private long createVehicle(String plate) throws Exception {
        return idOf(postVehicle(plate).andExpect(status().isCreated()).andReturn().getResponse().getContentAsString());
    }

    private long createOrder(long vehicleId) throws Exception {
        String json = mockMvc.perform(post("/vehicles/" + vehicleId + "/orders").header(KEY_HEADER, KEY))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return idOf(json);
    }

    private long createWorkItem(long orderId) throws Exception {
        String json = mockMvc.perform(post("/orders/" + orderId + "/work-items").header(KEY_HEADER, KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"description\":\"Oil change\",\"estimatedHours\":1,\"mechanicName\":\"Stefan\"}"))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return idOf(json);
    }

    private long addPart(long orderId, int quantity, double unitPrice) throws Exception {
        String json = mockMvc.perform(post("/orders/" + orderId + "/parts").header(KEY_HEADER, KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Brake pad\",\"quantity\":" + quantity + ",\"unitPrice\":" + unitPrice + "}"))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return idOf(json);
    }

    private double revenueThisMonth() throws Exception {
        String json = mockMvc.perform(get("/reports/revenue-by-month").header(KEY_HEADER, KEY))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        LocalDate today = LocalDate.now();
        for (JsonNode row : objectMapper.readTree(json)) {
            if (row.get("year").asInt() == today.getYear() && row.get("month").asInt() == today.getMonthValue()) {
                return row.get("totalRevenue").asDouble();
            }
        }
        return 0.0;
    }

    private long idOf(String json) throws Exception {
        return objectMapper.readTree(json).get("id").asLong();
    }
}
