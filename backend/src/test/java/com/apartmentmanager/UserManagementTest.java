package com.apartmentmanager;

import com.apartmentmanager.model.Apartment;
import com.apartmentmanager.model.Role;
import com.apartmentmanager.model.User;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class UserManagementTest extends AbstractIntegrationTest {

    @Test
    void getAllUsersAsOwner_returnsAllSeedUsers() throws Exception {
        mockMvc.perform(get("/api/users").header("Authorization", bearer("owner")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(3))))
                .andExpect(jsonPath("$[*].username", hasItem("owner")))
                .andExpect(jsonPath("$[*].username", hasItem("admin")))
                .andExpect(jsonPath("$[*].username", hasItem("tenant")))
                .andExpect(jsonPath("$[0].password").doesNotExist());
    }

    @Test
    void getAllUsersAsAdmin_returns403() throws Exception {
        mockMvc.perform(get("/api/users").header("Authorization", bearer("admin")))
                .andExpect(status().isForbidden());
    }

    @Test
    void getAllUsersAsTenant_returns403() throws Exception {
        mockMvc.perform(get("/api/users").header("Authorization", bearer("tenant")))
                .andExpect(status().isForbidden());
    }

    @Test
    void createUserAsOwner_succeeds() throws Exception {
        mockMvc.perform(post("/api/users")
                        .header("Authorization", bearer("owner"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"newuser\",\"password\":\"pass123\",\"role\":\"TENANT\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("newuser"))
                .andExpect(jsonPath("$.role").value("TENANT"))
                .andExpect(jsonPath("$.password").doesNotExist());

        assert userRepository.findByUsername("newuser").isPresent();
        User created = userRepository.findByUsername("newuser").orElseThrow();
        assert created.getRole() == Role.TENANT;
    }

    @Test
    void createUserAsAdmin_returns403() throws Exception {
        mockMvc.perform(post("/api/users")
                        .header("Authorization", bearer("admin"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"newuser\",\"password\":\"pass\",\"role\":\"TENANT\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void createUserAsTenant_returns403() throws Exception {
        mockMvc.perform(post("/api/users")
                        .header("Authorization", bearer("tenant"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"newuser\",\"password\":\"pass\",\"role\":\"TENANT\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void createUserDuplicateUsername_returnsError() throws Exception {
        mockMvc.perform(post("/api/users")
                        .header("Authorization", bearer("owner"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"owner\",\"password\":\"pass\",\"role\":\"TENANT\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Username already exists"));
    }

    @Test
    void createUserAsOwnerWithAdminRole_succeeds() throws Exception {
        mockMvc.perform(post("/api/users")
                        .header("Authorization", bearer("owner"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"newadmin\",\"password\":\"admin123\",\"role\":\"ADMIN\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("ADMIN"));

        User created = userRepository.findByUsername("newadmin").orElseThrow();
        assert created.getRole() == Role.ADMIN;
    }

    @Test
    void assignApartmentAsOwner_succeeds() throws Exception {
        Apartment apt = createApartment("User Mgmt Apt");
        User tenant = userRepository.findByUsername("tenant").orElseThrow();

        mockMvc.perform(put("/api/users/" + tenant.getId() + "/apartment")
                        .header("Authorization", bearer("owner"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"apartmentId\":" + apt.getId() + "}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("tenant"));

        User updated = userRepository.findByUsername("tenant").orElseThrow();
        assert updated.getApartment() != null;
        assert updated.getApartment().getId().equals(apt.getId());
    }

    @Test
    void assignApartmentAsAdmin_returns403() throws Exception {
        mockMvc.perform(put("/api/users/1/apartment")
                        .header("Authorization", bearer("admin"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"apartmentId\":1}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void deleteUserAsOwner_succeeds() throws Exception {
        mockMvc.perform(post("/api/users")
                        .header("Authorization", bearer("owner"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"todelete\",\"password\":\"pass\",\"role\":\"TENANT\"}"))
                .andExpect(status().isOk());

        User toDelete = userRepository.findByUsername("todelete").orElseThrow();

        mockMvc.perform(delete("/api/users/" + toDelete.getId())
                        .header("Authorization", bearer("owner")))
                .andExpect(status().isNoContent());

        assert userRepository.findByUsername("todelete").isEmpty();
    }

    @Test
    void deleteUserAsAdmin_returns403() throws Exception {
        mockMvc.perform(delete("/api/users/1")
                        .header("Authorization", bearer("admin")))
                .andExpect(status().isForbidden());
    }

    @Test
    void ownerCannotDeleteOtherOwners() throws Exception {
        User owner2 = userRepository.findByUsername("owner").orElseThrow();

        mockMvc.perform(delete("/api/users/" + owner2.getId())
                        .header("Authorization", bearer("owner")))
                .andExpect(status().isNoContent());
    }
}
