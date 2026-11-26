package com.example.CarService;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** The read endpoints the frontend needs: single vehicle, paged/searchable list, orders, parts, sorted revenue. */
@SpringBootTest
@AutoConfigureMockMvc
class ReadEndpointTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    // ---- vehicles ----

    @Test
    void getVehicleByIdIncludingOldFlag() throws Exception {
        String plate = uniquePlate();
        long id = createVehicle(plate, 2015);

        mockMvc.perform(get("/vehicles/" + id).with(jwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.licensePlate").value(plate))
                .andExpect(jsonPath("$.old").value(true));

        mockMvc.perform(get("/vehicles/999999").with(jwt())).andExpect(status().isNotFound());
    }

    @Test
    void createdVehicleHasOldFlagSet() throws Exception {
        postVehicle(uniquePlate(), 2010).andExpect(jsonPath("$.old").value(true));
        postVehicle(uniquePlate(), 2024).andExpect(jsonPath("$.old").value(false));
    }

    @Test
    void vehicleListReturnsPageInfo() throws Exception {
        mockMvc.perform(get("/vehicles?page=0&size=2").with(jwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(2))
                .andExpect(jsonPath("$.totalElements").isNumber())
                .andExpect(jsonPath("$.totalPages").isNumber());
    }

    @Test
    void vehicleListSearchIsCaseInsensitive() throws Exception {
        String plate = uniquePlate();
        createVehicle(plate, 2021);

        mockMvc.perform(get("/vehicles").param("search", plate.toLowerCase()).with(jwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].licensePlate").value(plate));
    }

    @Test
    void sortingByUnknownFieldIs400() throws Exception {
        mockMvc.perform(get("/vehicles?sort=banana").with(jwt()))
                .andExpect(status().isBadRequest());
    }

    // ---- orders ----

    @Test
    void orderResponsesIncludeVehicle() throws Exception {
        String plate = uniquePlate();
        long vehicleId = createVehicle(plate, 2021);
        long orderId = createOrder(vehicleId);

        mockMvc.perform(get("/orders/" + orderId).with(jwt()))
                .andExpect(jsonPath("$.vehicleId").value(vehicleId))
                .andExpect(jsonPath("$.licensePlate").value(plate));
    }

    @Test
    void ordersCanBeFilteredByStatus() throws Exception {
        long orderId = createOrder(createVehicle(uniquePlate(), 2021));

        JsonNode open = getJson("/orders?status=OPEN");
        assertThat(open.findValuesAsText("status")).containsOnly("OPEN");
        assertThat(open.findValues("id").stream().map(JsonNode::asLong)).contains(orderId);

        JsonNode completed = getJson("/orders?status=completed"); // case-insensitive
        assertThat(completed.findValuesAsText("status")).containsOnly("COMPLETED");

        mockMvc.perform(get("/orders?status=BROKEN").with(jwt())).andExpect(status().isBadRequest());
    }

    @Test
    void ordersOfDeletedVehicleAreHidden() throws Exception {
        long vehicleId = createVehicle(uniquePlate(), 2021);
        long orderId = createOrder(vehicleId);
        mockMvc.perform(delete("/vehicles/" + vehicleId).with(jwt())).andExpect(status().isNoContent());

        assertThat(getJson("/orders").findValues("id").stream().map(JsonNode::asLong)).doesNotContain(orderId);
    }

    // ---- parts ----

    @Test
    void partsOfOrderAreListedWithoutDeletedOnes() throws Exception {
        long orderId = createOrder(createVehicle(uniquePlate(), 2021));
        addPart(orderId, "Oil filter");
        long removed = addPart(orderId, "Wrong part");
        mockMvc.perform(delete("/parts/" + removed).with(jwt())).andExpect(status().isNoContent());

        mockMvc.perform(get("/orders/" + orderId + "/parts").with(jwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].name").value("Oil filter"));

        mockMvc.perform(get("/orders/999999/parts").with(jwt())).andExpect(status().isNotFound());
    }

    // ---- revenue ----

    @Test
    void revenueIsSortedByMonth() throws Exception {
        // make sure there is revenue in the current month too, next to the seeded older months
        long orderId = createOrder(createVehicle(uniquePlate(), 2021));
        addPart(orderId, "Battery");
        mockMvc.perform(patch("/orders/" + orderId + "/complete").with(jwt())).andExpect(status().isOk());

        JsonNode rows = getJson("/reports/revenue-by-month");
        assertThat(rows.size()).isGreaterThan(1);
        for (int i = 1; i < rows.size(); i++) {
            int previous = rows.get(i - 1).get("year").asInt() * 12 + rows.get(i - 1).get("month").asInt();
            int current = rows.get(i).get("year").asInt() * 12 + rows.get(i).get("month").asInt();
            assertThat(current).isGreaterThan(previous);
        }
    }

    // ---- helpers ----

    private String uniquePlate() {
        return "R-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    private org.springframework.test.web.servlet.ResultActions postVehicle(String plate, int year) throws Exception {
        String body = "{\"licensePlate\":\"" + plate + "\",\"ownerName\":\"Test Owner\",\"make\":\"Opel\",\"model\":\"Astra\",\"year\":" + year + "}";
        return mockMvc.perform(post("/vehicles").with(jwt()).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated());
    }

    private long createVehicle(String plate, int year) throws Exception {
        return idOf(postVehicle(plate, year).andReturn().getResponse().getContentAsString());
    }

    private long createOrder(long vehicleId) throws Exception {
        return idOf(mockMvc.perform(post("/vehicles/" + vehicleId + "/orders").with(jwt()))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString());
    }

    private long addPart(long orderId, String name) throws Exception {
        return idOf(mockMvc.perform(post("/orders/" + orderId + "/parts").with(jwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"" + name + "\",\"quantity\":1,\"unitPrice\":40}"))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString());
    }

    private JsonNode getJson(String url) throws Exception {
        return objectMapper.readTree(mockMvc.perform(get(url).with(jwt()))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
    }

    private long idOf(String json) throws Exception {
        return objectMapper.readTree(json).get("id").asLong();
    }
}
