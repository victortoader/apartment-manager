package com.example.demo;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class SecurityTest extends AbstractIntegrationTest {

    @Test
    void unauthenticatedGetApartments_returns403() throws Exception {
        mockMvc.perform(get("/api/apartments"))
                .andExpect(status().isForbidden());
    }

    @Test
    void unauthenticatedGetUsers_returns403() throws Exception {
        mockMvc.perform(get("/api/users"))
                .andExpect(status().isForbidden());
    }

    @Test
    void unauthenticatedPostApartments_returns403() throws Exception {
        mockMvc.perform(post("/api/apartments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"X\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void optionsRequest_bypassesJwtFilter() throws Exception {
        mockMvc.perform(options("/api/apartments")
                        .header("Origin", "http://localhost:3000")
                        .header("Access-Control-Request-Method", "GET"))
                .andExpect(status().isOk());
    }

    @Test
    void malformedAuthHeader_returns403() throws Exception {
        mockMvc.perform(get("/api/apartments")
                        .header("Authorization", "Token abc123"))
                .andExpect(status().isForbidden());
    }

    @Test
    void emptyBearerToken_returns403() throws Exception {
        mockMvc.perform(get("/api/apartments")
                        .header("Authorization", "Bearer "))
                .andExpect(status().isForbidden());
    }

    @Test
    void expiredToken_returns403() throws Exception {
        mockMvc.perform(get("/api/apartments")
                        .header("Authorization", "Bearer eyJhbGciOiJIUzI1NiJ9.invalid"))
                .andExpect(status().isForbidden());
    }

    @Test
    void photosEndpoint_isAccessibleWithoutAuth() throws Exception {
        mockMvc.perform(get("/api/apartments/photos/somefile.jpg"))
                .andExpect(status().isNotFound());
    }

    @Test
    void protocolsEndpoint_isAccessibleWithoutAuth() throws Exception {
        mockMvc.perform(get("/api/apartments/protocols/somefile.pdf"))
                .andExpect(status().isNotFound());
    }
}
