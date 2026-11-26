package com.example.CarService;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.util.UUID;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Work-item statuses for the workshop board: TODO -> IN_PROGRESS -> DONE, in any direction. */
@SpringBootTest
@AutoConfigureMockMvc
class WorkItemStatusTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void newWorkItemStartsAsTodo() throws Exception {
        long orderId = createOrder();
        mockMvc.perform(post("/orders/" + orderId + "/work-items").with(jwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"description\":\"Tyre swap\",\"estimatedHours\":1,\"mechanicName\":\"Stefan\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("TODO"))
                .andExpect(jsonPath("$.orderId").value(orderId));
    }

    @Test
    void statusCanMoveInAnyDirection() throws Exception {
        long itemId = createWorkItem(createOrder());

        changeStatus(itemId, "IN_PROGRESS").andExpect(status().isOk()).andExpect(jsonPath("$.status").value("IN_PROGRESS"));
        changeStatus(itemId, "DONE").andExpect(status().isOk()).andExpect(jsonPath("$.status").value("DONE"));
        changeStatus(itemId, "TODO").andExpect(status().isOk()).andExpect(jsonPath("$.status").value("TODO"));
    }

    @Test
    void invalidOrMissingStatusIs400() throws Exception {
        long itemId = createWorkItem(createOrder());

        changeStatus(itemId, "FLYING").andExpect(status().isBadRequest());
        mockMvc.perform(patch("/work-items/" + itemId + "/status").with(jwt())
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.status").exists());
        changeStatus(999999, "DONE").andExpect(status().isNotFound());
    }

    @Test
    void todoAndInProgressBothBlockCompletion() throws Exception {
        long orderId = createOrder();
        long itemId = createWorkItem(orderId); // TODO

        mockMvc.perform(patch("/orders/" + orderId + "/complete").with(jwt()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("Cannot complete order while work items are not done"));

        changeStatus(itemId, "IN_PROGRESS");
        mockMvc.perform(patch("/orders/" + orderId + "/complete").with(jwt())).andExpect(status().isConflict());

        changeStatus(itemId, "DONE");
        mockMvc.perform(patch("/orders/" + orderId + "/complete").with(jwt())).andExpect(status().isOk());

        // the order is now locked
        changeStatus(itemId, "TODO").andExpect(status().isConflict());
    }

    @Test
    void boardListsItemsOfOpenOrdersWithOrderAndPlate() throws Exception {
        String plate = "B-" + UUID.randomUUID().toString().substring(0, 8);
        long vehicleId = createVehicle(plate);
        long openOrder = createOrder(vehicleId);
        long openItem = createWorkItem(openOrder);
        long deletedItem = createWorkItem(openOrder);
        mockMvc.perform(delete("/work-items/" + deletedItem).with(jwt())).andExpect(status().isNoContent());

        long completedOrder = createOrder(vehicleId);
        long completedItem = createWorkItem(completedOrder);
        changeStatus(completedItem, "DONE");
        mockMvc.perform(patch("/orders/" + completedOrder + "/complete").with(jwt())).andExpect(status().isOk());

        JsonNode board = getJson("/work-items?orderStatus=open");
        assertThat(ids(board)).contains(openItem).doesNotContain(deletedItem, completedItem);
        for (JsonNode card : board) {
            if (card.get("id").asLong() == openItem) {
                assertThat(card.get("orderId").asLong()).isEqualTo(openOrder);
                assertThat(card.get("licensePlate").asText()).isEqualTo(plate);
            }
        }

        // without a filter, items of completed orders are included too
        assertThat(ids(getJson("/work-items"))).contains(openItem, completedItem);

        mockMvc.perform(get("/work-items?orderStatus=WRONG").with(jwt())).andExpect(status().isBadRequest());
    }

    // ---- helpers ----

    private ResultActions changeStatus(long itemId, String status) throws Exception {
        return mockMvc.perform(patch("/work-items/" + itemId + "/status").with(jwt())
                .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"" + status + "\"}"));
    }

    private long createVehicle(String plate) throws Exception {
        String body = "{\"licensePlate\":\"" + plate + "\",\"ownerName\":\"Test Owner\",\"make\":\"Kia\",\"model\":\"Ceed\",\"year\":2022}";
        return idOf(mockMvc.perform(post("/vehicles").with(jwt()).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString());
    }

    private long createOrder() throws Exception {
        return createOrder(createVehicle("B-" + UUID.randomUUID().toString().substring(0, 8)));
    }

    private long createOrder(long vehicleId) throws Exception {
        return idOf(mockMvc.perform(post("/vehicles/" + vehicleId + "/orders").with(jwt()))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString());
    }

    private long createWorkItem(long orderId) throws Exception {
        return idOf(mockMvc.perform(post("/orders/" + orderId + "/work-items").with(jwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"description\":\"Inspection\",\"estimatedHours\":2,\"mechanicName\":\"Bogdan\"}"))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString());
    }

    private JsonNode getJson(String url) throws Exception {
        return objectMapper.readTree(mockMvc.perform(get(url).with(jwt()))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
    }

    private java.util.List<Long> ids(JsonNode array) {
        return array.findValues("id").stream().map(JsonNode::asLong).toList();
    }

    private long idOf(String json) throws Exception {
        return objectMapper.readTree(json).get("id").asLong();
    }
}
