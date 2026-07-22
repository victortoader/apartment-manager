package com.apartmentmanager;

import com.apartmentmanager.model.Apartment;
import com.apartmentmanager.model.User;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class AuthTest extends AbstractIntegrationTest {

    private static final String TEST_PASSWORD = "admin";

    @Test
    void loginOwner_returnsTokenAndRole() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"owner\",\"password\":\"" + TEST_PASSWORD + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.username").value("owner"))
                .andExpect(jsonPath("$.role").value("OWNER"));
    }

    @Test
    void loginAdmin_returnsTokenAndRole() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"admin\",\"password\":\"" + TEST_PASSWORD + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.role").value("ADMIN"));
    }

    @Test
    void loginTenant_returnsTokenWithApartmentId() throws Exception {
        Apartment apt = createApartment("Tenant Apt");
        User tenant = userRepository.findByUsername("tenant").orElseThrow();
        tenant.setApartment(apt);
        userRepository.save(tenant);

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"tenant\",\"password\":\"" + TEST_PASSWORD + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("TENANT"))
                .andExpect(jsonPath("$.apartmentId").value(apt.getId()));
    }

    @Test
    void loginTenantWithoutApartment_noApartmentId() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"tenant\",\"password\":\"" + TEST_PASSWORD + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.apartmentId").doesNotExist());
    }

    @Test
    void loginInvalidPassword_returns401() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"owner\",\"password\":\"wrong\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Invalid credentials"));
    }

    @Test
    void loginUnknownUser_returns401() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"nobody\",\"password\":\"pass\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void loginMissingFields_returns401() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void meWithValidToken_returnsUserInfo() throws Exception {
        mockMvc.perform(get("/api/auth/me")
                        .header("Authorization", bearer("owner")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("owner"))
                .andExpect(jsonPath("$.role").value("OWNER"));
    }

    @Test
    void meAsTenantWithApartment_returnsApartmentId() throws Exception {
        Apartment apt = createApartment("My Apt");
        User tenant = userRepository.findByUsername("tenant").orElseThrow();
        tenant.setApartment(apt);
        userRepository.save(tenant);

        mockMvc.perform(get("/api/auth/me")
                        .header("Authorization", bearer("tenant")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("TENANT"))
                .andExpect(jsonPath("$.apartmentId").value(apt.getId()));
    }

    @Test
    void meWithInvalidToken_returns403() throws Exception {
        mockMvc.perform(get("/api/auth/me")
                        .header("Authorization", "Bearer invalidtoken"))
                .andExpect(status().isForbidden());
    }

    @Test
    void meWithoutToken_returns403() throws Exception {
        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isForbidden());
    }
}
