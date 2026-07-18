package com.example.demo;

import com.example.demo.model.Apartment;
import com.example.demo.model.HandoverProtocol;
import com.example.demo.model.User;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class OwnerFlowTest extends AbstractIntegrationTest {

    @Test
    void fullOwnerWorkflow() throws Exception {
        mockMvc.perform(post("/api/apartments")
                        .header("Authorization", bearer("owner"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Owner Apt\",\"description\":\"Great place\",\"location\":\"Cluj\",\"price\":1200,\"rooms\":3,\"area\":75}"))
                .andExpect(status().isOk());

        Apartment created = apartmentRepository.findAll().stream()
                .filter(a -> a.getTitle().equals("Owner Apt"))
                .findFirst().orElseThrow();

        mockMvc.perform(get("/api/apartments").header("Authorization", bearer("owner")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(greaterThanOrEqualTo(1)));

        MockMultipartFile photo = new MockMultipartFile(
                "file", "owner_photo.jpg", "image/jpeg", "photo data".getBytes());
        mockMvc.perform(multipart("/api/apartments/" + created.getId() + "/photos").file(photo)
                        .header("Authorization", bearer("owner")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.photoPaths", hasSize(1)));

        MockMultipartFile proto = new MockMultipartFile(
                "file", "handover.pdf", "application/pdf", "pdf data".getBytes());
        mockMvc.perform(multipart("/api/apartments/" + created.getId() + "/protocols").file(proto)
                        .param("documentType", "HANDOVER_PROTOCOL")
                        .header("Authorization", bearer("owner")))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/apartments/" + created.getId() + "/protocols")
                        .header("Authorization", bearer("owner")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));

        mockMvc.perform(post("/api/users")
                        .header("Authorization", bearer("owner"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"newtenant\",\"password\":\"pass\",\"role\":\"TENANT\"}"))
                .andExpect(status().isOk());

        User newTenant = userRepository.findByUsername("newtenant").orElseThrow();

        mockMvc.perform(put("/api/users/" + newTenant.getId() + "/apartment")
                        .header("Authorization", bearer("owner"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"apartmentId\":" + created.getId() + "}"))
                .andExpect(status().isOk());
        entityManager.flush();
        entityManager.clear();

        mockMvc.perform(get("/api/apartments/" + created.getId())
                        .header("Authorization", bearer("owner")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tenant").value("newtenant"));

        HandoverProtocol protoEntity = protocolRepository.findByApartmentId(created.getId()).get(0);
        mockMvc.perform(delete("/api/apartments/protocols/" + protoEntity.getId())
                        .header("Authorization", bearer("owner")))
                .andExpect(status().isNoContent());

        mockMvc.perform(delete("/api/apartments/" + created.getId())
                        .header("Authorization", bearer("owner")))
                .andExpect(status().isNoContent());

        assert apartmentRepository.findById(created.getId()).isEmpty();
    }
}
