package com.example.demo;

import com.example.demo.model.Apartment;
import com.example.demo.model.User;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class TenantFlowTest extends AbstractIntegrationTest {

    @Test
    void fullTenantWorkflow() throws Exception {
        Apartment apt = createApartment("Tenant Workflow Apt");
        User tenant = userRepository.findByUsername("tenant").orElseThrow();
        tenant.setApartment(apt);
        userRepository.save(tenant);
        entityManager.flush();
        entityManager.clear();

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"tenant\",\"password\":\"tenant\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        mockMvc.perform(get("/api/apartments").header("Authorization", bearer("tenant")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].tenant").value("tenant"));

        mockMvc.perform(get("/api/apartments/" + apt.getId())
                        .header("Authorization", bearer("tenant")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tenant").value("tenant"));

        MockMultipartFile file = new MockMultipartFile(
                "file", "my_photo.jpg", "image/jpeg", "content".getBytes());
        mockMvc.perform(multipart("/api/apartments/" + apt.getId() + "/photos").file(file)
                        .header("Authorization", bearer("tenant")))
                .andExpect(status().isOk());

        MockMultipartFile proto = new MockMultipartFile(
                "file", "my_doc.pdf", "application/pdf", "pdf".getBytes());
        mockMvc.perform(multipart("/api/apartments/" + apt.getId() + "/protocols").file(proto)
                        .param("documentType", "OTHER")
                        .header("Authorization", bearer("tenant")))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/apartments/" + apt.getId() + "/protocols")
                        .header("Authorization", bearer("tenant")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));

        mockMvc.perform(delete("/api/apartments/" + apt.getId())
                        .header("Authorization", bearer("tenant")))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/apartments")
                        .header("Authorization", bearer("tenant"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"X\",\"price\":100}"))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/users").header("Authorization", bearer("tenant")))
                .andExpect(status().isForbidden());
    }
}
