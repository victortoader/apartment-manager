package com.apartmentmanager;

import com.apartmentmanager.model.Apartment;
import com.apartmentmanager.model.HandoverProtocol;
import com.apartmentmanager.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MvcResult;
import tools.jackson.databind.JsonNode;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class ProtocolTest extends AbstractIntegrationTest {

    private Apartment apt;

    @BeforeEach
    void setUp() {
        apt = createApartment("Protocol Apt");
    }

    private Long uploadProtocol(String docType) throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "document.pdf", "application/pdf", "pdf content".getBytes());

        MvcResult result = mockMvc.perform(multipart("/api/apartments/" + apt.getId() + "/protocols").file(file)
                        .param("documentType", docType)
                        .header("Authorization", bearer("owner")))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode json = objectMapper.readTree(result.getResponse().getContentAsString());
        return json.get("id").asLong();
    }

    @Test
    void uploadHandoverProtocol_succeeds() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "protocol.pdf", "application/pdf", "pdf content".getBytes());

        mockMvc.perform(multipart("/api/apartments/" + apt.getId() + "/protocols").file(file)
                        .param("documentType", "HANDOVER_PROTOCOL")
                        .header("Authorization", bearer("owner")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.originalName").value("protocol.pdf"))
                .andExpect(jsonPath("$.documentType").value("HANDOVER_PROTOCOL"))
                .andExpect(jsonPath("$.fileName").isNotEmpty())
                .andExpect(jsonPath("$.contentType").value("application/pdf"))
                .andExpect(jsonPath("$.id").isNumber());
    }

    @Test
    void uploadBillsDocument_succeeds() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "bills.pdf", "application/pdf", "content".getBytes());

        mockMvc.perform(multipart("/api/apartments/" + apt.getId() + "/protocols").file(file)
                        .param("documentType", "BILLS")
                        .header("Authorization", bearer("owner")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.documentType").value("BILLS"));
    }

    @Test
    void uploadPhotosDocument_succeeds() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "photos.pdf", "application/pdf", "content".getBytes());

        mockMvc.perform(multipart("/api/apartments/" + apt.getId() + "/protocols").file(file)
                        .param("documentType", "PHOTOS")
                        .header("Authorization", bearer("owner")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.documentType").value("PHOTOS"));
    }

    @Test
    void uploadOtherDocument_succeeds() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "other.pdf", "application/pdf", "content".getBytes());

        mockMvc.perform(multipart("/api/apartments/" + apt.getId() + "/protocols").file(file)
                        .param("documentType", "OTHER")
                        .header("Authorization", bearer("owner")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.documentType").value("OTHER"));
    }

    @Test
    void uploadProtocolAsAdmin_succeeds() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "admin_doc.pdf", "application/pdf", "content".getBytes());

        mockMvc.perform(multipart("/api/apartments/" + apt.getId() + "/protocols").file(file)
                        .param("documentType", "BILLS")
                        .header("Authorization", bearer("admin")))
                .andExpect(status().isOk());
    }

    @Test
    void uploadProtocolAsTenantOwnApartment_succeeds() throws Exception {
        User tenant = userRepository.findByUsername("tenant").orElseThrow();
        tenant.setApartment(apt);
        userRepository.save(tenant);

        MockMultipartFile file = new MockMultipartFile(
                "file", "tenant_doc.pdf", "application/pdf", "content".getBytes());

        mockMvc.perform(multipart("/api/apartments/" + apt.getId() + "/protocols").file(file)
                        .param("documentType", "OTHER")
                        .header("Authorization", bearer("tenant")))
                .andExpect(status().isOk());
    }

    @Test
    void uploadProtocolAsTenantOtherApartment_returns403() throws Exception {
        User tenant = userRepository.findByUsername("tenant").orElseThrow();
        tenant.setApartment(apt);
        userRepository.save(tenant);

        Apartment other = createApartment("Other");
        MockMultipartFile file = new MockMultipartFile(
                "file", "doc.pdf", "application/pdf", "content".getBytes());

        mockMvc.perform(multipart("/api/apartments/" + other.getId() + "/protocols").file(file)
                        .param("documentType", "OTHER")
                        .header("Authorization", bearer("tenant")))
                .andExpect(status().isForbidden());
    }

    @Test
    void getProtocolsAsOwner_returnsList() throws Exception {
        uploadProtocol("HANDOVER_PROTOCOL");
        uploadProtocol("BILLS");

        mockMvc.perform(get("/api/apartments/" + apt.getId() + "/protocols")
                        .header("Authorization", bearer("owner")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].originalName").isNotEmpty())
                .andExpect(jsonPath("$[0].documentType").isNotEmpty())
                .andExpect(jsonPath("$[0].createdAt").isNotEmpty());
    }

    @Test
    void getProtocolsAsTenantOwnApartment_returnsList() throws Exception {
        User tenant = userRepository.findByUsername("tenant").orElseThrow();
        tenant.setApartment(apt);
        userRepository.save(tenant);

        uploadProtocol("BILLS");

        mockMvc.perform(get("/api/apartments/" + apt.getId() + "/protocols")
                        .header("Authorization", bearer("tenant")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));
    }

    @Test
    void getProtocolsAsTenantOtherApartment_returnsEmpty() throws Exception {
        User tenant = userRepository.findByUsername("tenant").orElseThrow();
        tenant.setApartment(apt);
        userRepository.save(tenant);

        Apartment other = createApartment("Other");
        uploadProtocol("BILLS");

        mockMvc.perform(get("/api/apartments/" + other.getId() + "/protocols")
                        .header("Authorization", bearer("tenant")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void deleteProtocolAsOwner_succeeds() throws Exception {
        Long protocolId = uploadProtocol("HANDOVER_PROTOCOL");

        mockMvc.perform(delete("/api/apartments/protocols/" + protocolId)
                        .header("Authorization", bearer("owner")))
                .andExpect(status().isNoContent());

        assert protocolRepository.findById(protocolId).isEmpty();
    }

    @Test
    void deleteProtocolAsAdmin_returns403() throws Exception {
        Long protocolId = uploadProtocol("OTHER");

        mockMvc.perform(delete("/api/apartments/protocols/" + protocolId)
                        .header("Authorization", bearer("admin")))
                .andExpect(status().isForbidden());
    }

    @Test
    void getProtocolFile_isPublicEndpoint() throws Exception {
        uploadProtocol("HANDOVER_PROTOCOL");

        HandoverProtocol proto = protocolRepository.findByApartmentId(apt.getId()).get(0);

        mockMvc.perform(get("/api/apartments/protocols/" + proto.getFileName()))
                .andExpect(status().isOk());
    }

    @Test
    void getProtocolFile_nonExistent_returns404() throws Exception {
        mockMvc.perform(get("/api/apartments/protocols/nonexistent.pdf"))
                .andExpect(status().isNotFound());
    }
}
