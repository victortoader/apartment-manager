package com.example.demo;

import com.example.demo.model.Apartment;
import com.example.demo.model.HandoverProtocol;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class AdminFlowTest extends AbstractIntegrationTest {

    @Test
    void fullAdminWorkflow() throws Exception {
        mockMvc.perform(post("/api/apartments")
                        .header("Authorization", bearer("admin"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Admin Apt\",\"price\":900,\"rooms\":2,\"area\":45}"))
                .andExpect(status().isOk());

        Apartment created = apartmentRepository.findAll().stream()
                .filter(a -> a.getTitle().equals("Admin Apt"))
                .findFirst().orElseThrow();

        mockMvc.perform(get("/api/apartments").header("Authorization", bearer("admin")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(1))));

        MockMultipartFile photo = new MockMultipartFile(
                "file", "admin_photo.jpg", "image/jpeg", "data".getBytes());
        mockMvc.perform(multipart("/api/apartments/" + created.getId() + "/photos").file(photo)
                        .header("Authorization", bearer("admin")))
                .andExpect(status().isOk());

        MockMultipartFile proto = new MockMultipartFile(
                "file", "admin_doc.pdf", "application/pdf", "data".getBytes());
        mockMvc.perform(multipart("/api/apartments/" + created.getId() + "/protocols").file(proto)
                        .param("documentType", "BILLS")
                        .header("Authorization", bearer("admin")))
                .andExpect(status().isOk());

        mockMvc.perform(delete("/api/apartments/" + created.getId())
                        .header("Authorization", bearer("admin")))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/users").header("Authorization", bearer("admin")))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/users")
                        .header("Authorization", bearer("admin"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"x\",\"password\":\"y\",\"role\":\"TENANT\"}"))
                .andExpect(status().isForbidden());

        HandoverProtocol protoEntity = protocolRepository.findByApartmentId(created.getId()).get(0);
        mockMvc.perform(delete("/api/apartments/protocols/" + protoEntity.getId())
                        .header("Authorization", bearer("admin")))
                .andExpect(status().isForbidden());
    }
}
