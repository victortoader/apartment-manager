package com.apartmentmanager;

import com.apartmentmanager.model.Apartment;
import com.apartmentmanager.model.HandoverProtocol;
import com.apartmentmanager.model.Inspection;
import com.apartmentmanager.repository.InspectionRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import tools.jackson.databind.JsonNode;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class InspectionTest extends AbstractIntegrationTest {

    @Autowired
    private InspectionRepository inspectionRepository;

    @Test
    void saveAndGeneratePdf_succeeds() throws Exception {
        Apartment apt = createApartment("Inspection Apt");

        String confirmationsJson = "[\\\"Schluessel erhalten\\\",\\\"Wassermesserstand notiert\\\"]";
        String signaturesJson = "[{\\\"role\\\":\\\"Vermieter\\\",\\\"city\\\":\\\"Berlin\\\",\\\"date\\\":\\\"30.07.2026\\\",\\\"printedName\\\":\\\"Hans Vermieter\\\"}]";
        String body = jsonResource("inspections/full-inspection.json").formatted(confirmationsJson, signaturesJson);

        var result = mockMvc.perform(post("/api/apartments/" + apt.getId() + "/inspections")
                        .header("Authorization", bearer("owner"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.documentType").value("Uebergabeprotokoll"))
                .andExpect(jsonPath("$.sections.length()").value(2))
                .andExpect(jsonPath("$.sections[0].rows.length()").value(2))
                .andExpect(jsonPath("$.sections[1].rows.length()").value(1))
                .andReturn();

        String json = result.getResponse().getContentAsString();

        Long inspectionId = objectMapper.readTree(json).get("id").asLong();

        mockMvc.perform(post("/api/inspections/" + inspectionId + "/generate")
                        .header("Authorization", bearer("owner")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.documentType").value("HANDOVER_PROTOCOL"))
                .andExpect(jsonPath("$.originalName").value(containsString("Uebergabeprotokoll")))
                .andExpect(jsonPath("$.fileName").isNotEmpty());

        HandoverProtocol protocol = protocolRepository.findByApartmentId(apt.getId()).stream().findFirst().orElseThrow();

        mockMvc.perform(get("/api/apartments/protocols/" + protocol.getFileName()))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "application/pdf"));

        Inspection saved = inspectionRepository.findById(inspectionId).orElseThrow();
        assert saved.getGeneratedProtocolId() != null;
        assert saved.getGeneratedProtocolId().equals(protocol.getId());
    }

    @Test
    void saveInspectionAsAdmin_succeeds() throws Exception {
        Apartment apt = createApartment("Admin Inspection Apt");

        mockMvc.perform(post("/api/apartments/" + apt.getId() + "/inspections")
                        .header("Authorization", bearer("admin"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"documentType\":\"Uebergabeprotokoll\",\"sections\":[]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").isNumber());
    }

    @Test
    void saveInspectionAsTenant_returns403() throws Exception {
        Apartment apt = createApartment("Tenant no access");

        mockMvc.perform(post("/api/apartments/" + apt.getId() + "/inspections")
                        .header("Authorization", bearer("tenant"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"documentType\":\"Uebergabeprotokoll\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void generatePdf_nonExistentInspection_returnsError() throws Exception {
        mockMvc.perform(post("/api/inspections/99999/generate")
                        .header("Authorization", bearer("owner")))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.error").isString());
    }

    @Test
    void addSectionViaUpdate_thenGeneratePdf_succeeds() throws Exception {
        Apartment apt = createApartment("Update Section Test");

        String body = jsonResource("inspections/inspection-one-section.json");

        var result = mockMvc.perform(post("/api/apartments/" + apt.getId() + "/inspections")
                        .header("Authorization", bearer("owner"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sections.length()").value(1))
                .andReturn();

        Long inspectionId = objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asLong();

        String updateBody = jsonResource("inspections/inspection-two-sections.json");

        var updateResult = mockMvc.perform(put("/api/inspections/" + inspectionId)
                        .header("Authorization", bearer("owner"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateBody))
                .andExpect(status().isOk())
                .andReturn();

        int sectionCount = objectMapper.readTree(updateResult.getResponse().getContentAsString())
                .get("sections").size();
        assert sectionCount == 2 : "Expected 2 sections after update, got " + sectionCount;

        mockMvc.perform(post("/api/inspections/" + inspectionId + "/generate")
                        .header("Authorization", bearer("owner")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").isNumber());
    }

    @Test
    void getInspectionsByApartment_returnsList() throws Exception {
        Apartment apt = createApartment("List Inspection Apt");

        mockMvc.perform(post("/api/apartments/" + apt.getId() + "/inspections")
                        .header("Authorization", bearer("owner"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"documentType\":\"First\",\"sections\":[]}"))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/apartments/" + apt.getId() + "/inspections")
                        .header("Authorization", bearer("owner"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"documentType\":\"Second\",\"sections\":[]}"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/apartments/" + apt.getId() + "/inspections")
                        .header("Authorization", bearer("owner")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }
}
