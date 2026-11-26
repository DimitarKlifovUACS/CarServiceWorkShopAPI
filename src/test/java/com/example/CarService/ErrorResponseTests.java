package com.example.CarService;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Every error must come back as JSON in the same shape (problem details), so the frontend can handle them all one way.
 * Uses the demo data from DataSeeder (order 4 has a work item in progress, vehicle ABC-123 exists).
 */
@SpringBootTest
@AutoConfigureMockMvc
class ErrorResponseTests {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void validationErrorListsEveryInvalidField() throws Exception {
        mockMvc.perform(post("/vehicles").with(jwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.detail").value("Validation failed"))
                .andExpect(jsonPath("$.fieldErrors.licensePlate").exists())
                .andExpect(jsonPath("$.fieldErrors.ownerName").exists())
                .andExpect(jsonPath("$.fieldErrors.make").exists())
                .andExpect(jsonPath("$.fieldErrors.model").exists())
                .andExpect(jsonPath("$.fieldErrors.year").exists());
    }

    @Test
    void missingOrderReturns404() throws Exception {
        mockMvc.perform(get("/orders/999999").with(jwt()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.detail").value("Order not found"))
                .andExpect(jsonPath("$.instance").value("/orders/999999"));
    }

    @Test
    void completingOrderWithWorkInProgressReturns409() throws Exception {
        mockMvc.perform(patch("/orders/4/complete").with(jwt()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.detail").value("Cannot complete order while work items are not done"));
    }

    @Test
    void duplicateLicensePlateReturns409() throws Exception {
        String body = """
                {"licensePlate":"ABC-123","ownerName":"Test Owner","make":"BMW","model":"320i","year":2020}
                """;
        mockMvc.perform(post("/vehicles").with(jwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("A vehicle with license plate ABC-123 already exists"));
    }

    @Test
    void malformedJsonReturns400() throws Exception {
        mockMvc.perform(post("/vehicles").with(jwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ not json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void textInsteadOfIdReturns400() throws Exception {
        mockMvc.perform(get("/orders/abc").with(jwt()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void unknownUrlReturns404NotServerError() throws Exception {
        mockMvc.perform(get("/does-not-exist").with(jwt()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }
}
