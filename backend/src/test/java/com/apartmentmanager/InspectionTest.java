package com.apartmentmanager;

import com.apartmentmanager.model.Apartment;
import com.apartmentmanager.model.HandoverProtocol;
import com.apartmentmanager.model.Inspection;
import com.apartmentmanager.repository.InspectionRepository;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import tools.jackson.databind.JsonNode;

import static org.junit.jupiter.api.Assertions.*;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class InspectionTest extends AbstractIntegrationTest {

    @Autowired
    private InspectionRepository inspectionRepository;

    @Test
    void saveAndGeneratePdf_succeeds() throws Exception {
        Apartment apt = createApartment("Inspection Apt");

        String confirmationsJson = jsonResource("inspections/confirmations.json");
        String signaturesJson = jsonResource("inspections/signatures.json");
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
        assertNotNull(saved.getGeneratedProtocolId(), "Inspection must have a generated protocol ID");
        assertEquals(protocol.getId(), saved.getGeneratedProtocolId(), "Protocol ID must match the generated protocol");
    }

    @Test
    void saveInspectionAsAdmin_succeeds() throws Exception {
        Apartment apt = createApartment("Admin Inspection Apt");

        mockMvc.perform(post("/api/apartments/" + apt.getId() + "/inspections")
                        .header("Authorization", bearer("admin"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonResource("inspections/inspection-empty.json")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").isNumber());
    }

    @Test
    void saveInspectionAsTenant_returns403() throws Exception {
        Apartment apt = createApartment("Tenant no access");

        mockMvc.perform(post("/api/apartments/" + apt.getId() + "/inspections")
                        .header("Authorization", bearer("tenant"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonResource("inspections/inspection-empty.json")))
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
        assertEquals(2, sectionCount, "Expected 2 sections after update");

        mockMvc.perform(post("/api/inspections/" + inspectionId + "/generate")
                        .header("Authorization", bearer("owner")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").isNumber());
    }

    @Test
    void generatePdf_containsFullFormStructure() throws Exception {
        Apartment apt = createApartment("Form Structure Apt");

        String confirmationsJson = jsonResource("inspections/confirmations-single.json");
        String signaturesJson = jsonResource("inspections/signatures.json");
        String body = jsonResource("inspections/full-inspection.json").formatted(confirmationsJson, signaturesJson);

        var res = mockMvc.perform(post("/api/apartments/" + apt.getId() + "/inspections")
                        .header("Authorization", bearer("owner"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk()).andReturn();
        Long inspectionId = objectMapper.readTree(res.getResponse().getContentAsString()).get("id").asLong();

        String labelsJson = jsonResource("inspections/labels-de.json");

        mockMvc.perform(post("/api/inspections/" + inspectionId + "/generate")
                        .header("Authorization", bearer("owner"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(labelsJson))
                .andExpect(status().isOk());

        HandoverProtocol protocol = protocolRepository.findByApartmentId(apt.getId()).get(0);
        byte[] pdfBytes = mockMvc.perform(get("/api/apartments/protocols/" + protocol.getFileName())
                        .header("Authorization", bearer("owner")))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsByteArray();

        try (PDDocument doc = Loader.loadPDF(pdfBytes)) {
            String text = new PDFTextStripper().getText(doc);
            assertTrue(text.contains("Ausziehender Mieter"), "PDF must contain the Moving out Tenant section");
            assertTrue(text.contains("Einziehender Mieter"), "PDF must contain the Moving in Tenant section");
            assertTrue(text.contains("Vorname") && text.contains("E-Mail"), "PDF must contain tenant fields");
            assertTrue(text.contains("Test GmbH") && text.contains("Musterstr. 1"), "PDF must contain tenant data");
            assertTrue(text.contains("Liegenschaft"), "PDF must contain property metadata");
            assertTrue(text.contains("Wohnzimmer") && text.contains("Kueche"), "PDF must contain inspection sections");
            assertTrue(text.contains("Bestaetigungen"), "PDF must contain confirmations");
            assertTrue(text.contains("Unterschriften"), "PDF must contain signatures");
        }
    }

    @Test
    void generatePdf_withEnglishLabels_usesEnglishLabels() throws Exception {
        Apartment apt = createApartment("English Form Apt");

        String confirmationsJson = jsonResource("inspections/confirmations-keys.json");
        String signaturesJson = jsonResource("inspections/signatures-landlord.json");
        String body = jsonResource("inspections/full-inspection.json").formatted(confirmationsJson, signaturesJson);

        var res = mockMvc.perform(post("/api/apartments/" + apt.getId() + "/inspections")
                        .header("Authorization", bearer("owner"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk()).andReturn();
        Long inspectionId = objectMapper.readTree(res.getResponse().getContentAsString()).get("id").asLong();

        String labelsJson = jsonResource("inspections/labels-en.json");

        mockMvc.perform(post("/api/inspections/" + inspectionId + "/generate")
                        .header("Authorization", bearer("owner"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(labelsJson))
                .andExpect(status().isOk());

        HandoverProtocol protocol = protocolRepository.findByApartmentId(apt.getId()).get(0);
        byte[] pdfBytes = mockMvc.perform(get("/api/apartments/protocols/" + protocol.getFileName())
                        .header("Authorization", bearer("owner")))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsByteArray();

        try (PDDocument doc = Loader.loadPDF(pdfBytes)) {
            String text = new PDFTextStripper().getText(doc);
            assertTrue(text.contains("Moving out Tenant"), "PDF must contain the English Moving out Tenant section");
            assertTrue(text.contains("Moving in Tenant"), "PDF must contain the English Moving in Tenant section");
            assertTrue(text.contains("First Name") && text.contains("Email"), "PDF must contain English tenant fields");
            assertTrue(text.contains("Property"), "PDF must contain English property metadata");
            assertTrue(text.contains("Confirmations"), "PDF must contain English confirmations");
            assertTrue(text.contains("Signatures"), "PDF must contain English signatures");
            assertTrue(text.contains("Page 1/"), "PDF must contain the English page footer");
        }
    }

    @Test
    void generatePdf_withMissingLabels_usesDefaults() throws Exception {
        Apartment apt = createApartment("Default Labels Apt");

        String confirmationsJson = jsonResource("inspections/confirmations-single.json");
        String signaturesJson = jsonResource("inspections/signatures.json");
        String body = jsonResource("inspections/full-inspection.json").formatted(confirmationsJson, signaturesJson);

        var res = mockMvc.perform(post("/api/apartments/" + apt.getId() + "/inspections")
                        .header("Authorization", bearer("owner"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk()).andReturn();
        Long inspectionId = objectMapper.readTree(res.getResponse().getContentAsString()).get("id").asLong();

        mockMvc.perform(post("/api/inspections/" + inspectionId + "/generate")
                        .header("Authorization", bearer("owner")))
                .andExpect(status().isOk());

        HandoverProtocol protocol = protocolRepository.findByApartmentId(apt.getId()).get(0);
        byte[] pdfBytes = mockMvc.perform(get("/api/apartments/protocols/" + protocol.getFileName())
                        .header("Authorization", bearer("owner")))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsByteArray();

        try (PDDocument doc = Loader.loadPDF(pdfBytes)) {
            String text = new PDFTextStripper().getText(doc);
            assertTrue(text.contains("Ausziehender Mieter"), "PDF should fall back to the default German label");
            assertTrue(text.contains("Bestaetigungen"), "PDF should use the default confirmations label");
            assertFalse(text.contains("photoIndex"), "PDF must not contain raw internal label keys");
        }
    }

    @Test
    void generatePdf_withoutSignatures_returnsError() throws Exception {
        Apartment apt = createApartment("No Signature Apt");

        String body = jsonResource("inspections/inspection-one-section.json");
        var res = mockMvc.perform(post("/api/apartments/" + apt.getId() + "/inspections")
                        .header("Authorization", bearer("owner"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andReturn();

        Long inspectionId = objectMapper.readTree(res.getResponse().getContentAsString()).get("id").asLong();

        mockMvc.perform(post("/api/inspections/" + inspectionId + "/generate")
                        .header("Authorization", bearer("owner")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value(containsStringIgnoringCase("signature")));
    }

    @Test
    void getInspectionsByApartment_returnsList() throws Exception {
        Apartment apt = createApartment("List Inspection Apt");

        mockMvc.perform(post("/api/apartments/" + apt.getId() + "/inspections")
                        .header("Authorization", bearer("owner"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonResource("inspections/inspection-empty.json")))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/apartments/" + apt.getId() + "/inspections")
                        .header("Authorization", bearer("owner"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonResource("inspections/inspection-empty.json")))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/apartments/" + apt.getId() + "/inspections")
                        .header("Authorization", bearer("owner")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }
}
