package com.apartmentmanager;

import com.apartmentmanager.model.Apartment;
import com.apartmentmanager.model.HandoverProtocol;
import com.apartmentmanager.repository.InspectionRowPhotoRepository;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDResources;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
class PdfPhotoReproTest extends AbstractIntegrationTest {

    private byte[] png() throws Exception {
        BufferedImage img = new BufferedImage(200, 150, BufferedImage.TYPE_INT_RGB);
        java.awt.Graphics2D g = img.createGraphics();
        g.setColor(java.awt.Color.RED); g.fillRect(0, 0, 200, 150);
        g.dispose();
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ImageIO.write(img, "png", bos);
        return bos.toByteArray();
    }

    @Autowired
    private InspectionRowPhotoRepository photoRepository;

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void photosAreEmbeddedInGeneratedPdf() throws Exception {
        Apartment apt = createApartment("Photo Apt");
        String body = jsonResource("inspections/inspection-simple.json");
        var res = mockMvc.perform(post("/api/apartments/" + apt.getId() + "/inspections")
                        .header("Authorization", bearer("owner"))
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk()).andReturn();
        long inspectionId = objectMapper.readTree(res.getResponse().getContentAsString()).get("id").asLong();

        var updRes = mockMvc.perform(get("/api/inspections/" + inspectionId)
                        .header("Authorization", bearer("owner")))
                .andExpect(status().isOk()).andReturn();
        long rowId = objectMapper.readTree(updRes.getResponse().getContentAsString())
                .get("sections").get(0).get("rows").get(0).get("id").asLong();

        mockMvc.perform(multipart("/api/inspections/rows/" + rowId + "/photos")
                        .file(new MockMultipartFile("file", "photo1.png", MediaType.IMAGE_PNG_VALUE, png()))
                        .header("Authorization", bearer("owner")))
                .andExpect(status().isOk());

        assertTrue(photoRepository.count() >= 1, "photo should be persisted");

        mockMvc.perform(post("/api/inspections/" + inspectionId + "/generate")
                        .header("Authorization", bearer("owner")))
                .andExpect(status().isOk());

        HandoverProtocol protocol = protocolRepository.findByApartmentId(apt.getId()).get(0);

        byte[] pdfBytes = mockMvc.perform(get("/api/apartments/protocols/" + protocol.getFileName())
                        .header("Authorization", bearer("owner")))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsByteArray();

        try (PDDocument doc = Loader.loadPDF(pdfBytes)) {
            int images = 0;
            for (PDPage page : doc.getPages()) {
                PDResources resources = page.getResources();
                for (org.apache.pdfbox.cos.COSName name : resources.getXObjectNames()) {
                    if (resources.getXObject(name) instanceof PDImageXObject) {
                        images++;
                    }
                }
            }
            assertTrue(images >= 1, "expected at least one embedded image in the PDF, got " + images);
        }
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void photoSurvivesFrontendStyleUpdate_thenAppearsInPdf() throws Exception {
        Apartment apt = createApartment("Photo Apt 2");
        String body = jsonResource("inspections/inspection-simple.json");
        var res = mockMvc.perform(post("/api/apartments/" + apt.getId() + "/inspections")
                        .header("Authorization", bearer("owner"))
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk()).andReturn();
        long inspectionId = objectMapper.readTree(res.getResponse().getContentAsString()).get("id").asLong();

        var updRes = mockMvc.perform(get("/api/inspections/" + inspectionId)
                        .header("Authorization", bearer("owner")))
                .andExpect(status().isOk()).andReturn();
        long rowId = objectMapper.readTree(updRes.getResponse().getContentAsString())
                .get("sections").get(0).get("rows").get(0).get("id").asLong();

        var uploadRes = mockMvc.perform(multipart("/api/inspections/rows/" + rowId + "/photos")
                        .file(new MockMultipartFile("file", "photo1.png", MediaType.IMAGE_PNG_VALUE, png()))
                        .header("Authorization", bearer("owner")))
                .andExpect(status().isOk()).andReturn();
        var photoJson = objectMapper.readTree(uploadRes.getResponse().getContentAsString());
        long photoId = photoJson.get("id").asLong();

        String updateBody = jsonResource("inspections/inspection-photo-update.json")
                .formatted(photoId, photoJson.get("storedFileName").asText(), photoJson.get("originalFileName").asText());

        mockMvc.perform(put("/api/inspections/" + inspectionId)
                        .header("Authorization", bearer("owner"))
                        .contentType(MediaType.APPLICATION_JSON).content(updateBody))
                .andExpect(status().isOk());

        var afterPut = mockMvc.perform(get("/api/inspections/" + inspectionId)
                        .header("Authorization", bearer("owner")))
                .andExpect(status().isOk()).andReturn();
        assertEquals(1, objectMapper.readTree(afterPut.getResponse().getContentAsString())
                .get("sections").get(0).get("rows").get(0).get("photos").size(),
                "photo should be re-linked to the row after update");

        mockMvc.perform(post("/api/inspections/" + inspectionId + "/generate")
                        .header("Authorization", bearer("owner")))
                .andExpect(status().isOk());

        HandoverProtocol protocol = protocolRepository.findByApartmentId(apt.getId()).get(0);
        byte[] pdfBytes = mockMvc.perform(get("/api/apartments/protocols/" + protocol.getFileName())
                        .header("Authorization", bearer("owner")))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsByteArray();

        try (PDDocument doc = Loader.loadPDF(pdfBytes)) {
            int images = 0;
            for (PDPage page : doc.getPages()) {
                PDResources resources = page.getResources();
                for (org.apache.pdfbox.cos.COSName name : resources.getXObjectNames()) {
                    if (resources.getXObject(name) instanceof PDImageXObject) {
                        images++;
                    }
                }
            }
            assertTrue(images >= 1, "expected image in PDF after frontend-style update, got " + images);
        }
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void downloadPhotosZip_containsAllPhotos() throws Exception {
        Apartment apt = createApartment("Zip Apt");
        String body = jsonResource("inspections/inspection-simple.json");
        var res = mockMvc.perform(post("/api/apartments/" + apt.getId() + "/inspections")
                        .header("Authorization", bearer("owner"))
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk()).andReturn();
        long inspectionId = objectMapper.readTree(res.getResponse().getContentAsString()).get("id").asLong();

        var updRes = mockMvc.perform(get("/api/inspections/" + inspectionId)
                        .header("Authorization", bearer("owner")))
                .andExpect(status().isOk()).andReturn();
        long rowId = objectMapper.readTree(updRes.getResponse().getContentAsString())
                .get("sections").get(0).get("rows").get(0).get("id").asLong();

        mockMvc.perform(multipart("/api/inspections/rows/" + rowId + "/photos")
                        .file(new MockMultipartFile("file", "photo1.png", MediaType.IMAGE_PNG_VALUE, png()))
                        .header("Authorization", bearer("owner")))
                .andExpect(status().isOk());

        var zipResp = mockMvc.perform(get("/api/inspections/" + inspectionId + "/photos")
                        .header("Authorization", bearer("owner")))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", org.hamcrest.Matchers.containsString(".zip")))
                .andReturn();

        int entries = 0;
        try (ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(zipResp.getResponse().getContentAsByteArray()))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                entries++;
            }
        }
        assertEquals(1, entries, "expected 1 photo in zip");
    }
}
