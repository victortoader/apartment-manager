package com.apartmentmanager;

import com.apartmentmanager.model.Apartment;
import com.apartmentmanager.model.Application;
import com.apartmentmanager.model.BillPayment;
import com.apartmentmanager.model.DocumentType;
import com.apartmentmanager.model.HandoverProtocol;
import com.apartmentmanager.model.Ticket;
import com.apartmentmanager.model.User;
import com.apartmentmanager.repository.ApplicationRepository;
import com.apartmentmanager.repository.BillPaymentRepository;
import com.apartmentmanager.repository.TicketRepository;
import com.apartmentmanager.service.BillPaymentService;
import com.apartmentmanager.service.HandoverProtocolService;
import com.apartmentmanager.service.PhotoStorageService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Access rules for every route that serves a stored file (security-fixes tickets 04, 05, 07-11).
 */
class FileAccessTest extends AbstractIntegrationTest {

    private static final byte[] PNG_BYTES = {
        (byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, 0, 0, 0, 13
    };

    @Autowired private PhotoStorageService photoStorageService;
    @Autowired private HandoverProtocolService protocolService;
    @Autowired private BillPaymentService billPaymentService;
    @Autowired private BillPaymentRepository billPaymentRepository;
    @Autowired private ApplicationRepository applicationRepository;
    @Autowired private TicketRepository ticketRepository;

    private Apartment aptA;
    private Apartment aptB;
    private final List<Path> filesToDelete = new ArrayList<>();

    @BeforeEach
    void setUp() {
        aptA = createApartment("Files Apt A");
        aptB = createApartment("Files Apt B");
        assignTenantTo(aptA);
    }

    @AfterEach
    void cleanUp() throws Exception {
        for (Path p : filesToDelete) {
            Files.deleteIfExists(p);
        }
    }

    private void assignTenantTo(Apartment apt) {
        User tenant = userRepository.findByUsername("tenant").orElseThrow();
        tenant.setApartment(apt);
        userRepository.save(tenant);
        entityManager.flush();
        entityManager.clear();
    }

    private String storeFile(String originalName) throws Exception {
        return photoStorageService.store(new MockMultipartFile("file", originalName, "image/png", PNG_BYTES));
    }

    private String apartmentPhoto(Apartment apt) throws Exception {
        String name = storeFile("photo.png");
        Apartment managed = apartmentRepository.findById(apt.getId()).orElseThrow();
        managed.getPhotoPaths().add(name);
        apartmentRepository.save(managed);
        entityManager.flush();
        entityManager.clear();
        return name;
    }

    private HandoverProtocol apartmentDocument(Apartment apt) throws Exception {
        Apartment managed = apartmentRepository.findById(apt.getId()).orElseThrow();
        HandoverProtocol protocol = protocolService.upload(apt.getId(),
                new MockMultipartFile("file", "protocol.pdf", "application/pdf", "pdf".getBytes()),
                DocumentType.HANDOVER_PROTOCOL, managed);
        entityManager.flush();
        entityManager.clear();
        return protocol;
    }

    private String billingDocument(Apartment apt) throws Exception {
        String name = UUID.randomUUID() + ".pdf";
        Path path = billPaymentService.load(name);
        Files.createDirectories(path.getParent());
        Files.write(path, "pdf".getBytes());
        filesToDelete.add(path);
        Apartment managed = apartmentRepository.findById(apt.getId()).orElseThrow();
        billPaymentRepository.save(new BillPayment("bill.pdf", name, "application/pdf", "Electricity", "bill", managed, null));
        entityManager.flush();
        entityManager.clear();
        return name;
    }

    private String applicantDocument(Apartment apt) throws Exception {
        String other = storeFile("id-card.png");
        String name = storeFile("payslip.png");
        Apartment managed = apartmentRepository.findById(apt.getId()).orElseThrow();
        applicationRepository.save(new Application(managed, "Applicant", "applicant@example.com", null,
                other + ", " + name, "id-card.png, payslip.png", null));
        entityManager.flush();
        entityManager.clear();
        return name;
    }

    private String ticketPhoto(Apartment apt, String createdBy) throws Exception {
        String name = storeFile("leak.png");
        Apartment managed = apartmentRepository.findById(apt.getId()).orElseThrow();
        User creator = userRepository.findByUsername(createdBy).orElseThrow();
        Ticket ticket = new Ticket("Leak", "Water", managed, creator);
        ticket.getPhotoPaths().add(name);
        ticketRepository.save(ticket);
        entityManager.flush();
        entityManager.clear();
        return name;
    }

    // ---- 04: billing document downloads ----

    @Nested
    class BillingDocumentDownload {

        @Test
        void tenantDownloadsBillingDocumentOfOtherApartment_returns403() throws Exception {
            String name = billingDocument(aptB);
            mockMvc.perform(get("/api/bills/" + name).header("Authorization", bearer("tenant")))
                    .andExpect(status().isForbidden());
        }

        @Test
        void tenantDownloadsBillingDocumentOfAssignedApartment_returns200() throws Exception {
            String name = billingDocument(aptA);
            mockMvc.perform(get("/api/bills/" + name).header("Authorization", bearer("tenant")))
                    .andExpect(status().isOk());
        }

        @Test
        void nameThatIsNotABillingDocument_returns404() throws Exception {
            String name = UUID.randomUUID() + ".pdf";
            Path path = billPaymentService.load(name);
            Files.createDirectories(path.getParent());
            Files.write(path, "not registered".getBytes());
            filesToDelete.add(path);

            mockMvc.perform(get("/api/bills/" + name).header("Authorization", bearer("owner")))
                    .andExpect(status().isNotFound());
        }

        @Test
        void adminDownloadsAnyBillingDocument_returns200() throws Exception {
            String name = billingDocument(aptB);
            mockMvc.perform(get("/api/bills/" + name).header("Authorization", bearer("admin")))
                    .andExpect(status().isOk());
        }
    }

    // ---- 05: applicant document downloads ----

    @Nested
    class ApplicantDocumentDownload {

        @Test
        void tenantDownloadsApplicantDocument_returns403() throws Exception {
            String name = applicantDocument(aptA);
            mockMvc.perform(get("/api/applications/files/" + name).header("Authorization", bearer("tenant")))
                    .andExpect(status().isForbidden());
        }

        @Test
        void adminAndOwnerDownloadApplicantDocument_return200() throws Exception {
            String name = applicantDocument(aptA);
            mockMvc.perform(get("/api/applications/files/" + name).header("Authorization", bearer("admin")))
                    .andExpect(status().isOk());
            mockMvc.perform(get("/api/applications/files/" + name).header("Authorization", bearer("owner")))
                    .andExpect(status().isOk());
        }

        @Test
        void nameThatIsNotAnApplicantDocument_returns404() throws Exception {
            String photo = apartmentPhoto(aptA);
            mockMvc.perform(get("/api/applications/files/" + photo).header("Authorization", bearer("owner")))
                    .andExpect(status().isNotFound());
        }

        @Test
        void withoutLogin_isRejected() throws Exception {
            String name = applicantDocument(aptA);
            mockMvc.perform(get("/api/applications/files/" + name))
                    .andExpect(status().isForbidden());
        }
    }

    // ---- 07: names outside the uploads folder ----

    @Nested
    class PathsOutsideUploads {

        @Test
        void photoStorageRefusesNamesOutsideUploads() {
            assertThrows(IllegalArgumentException.class, () -> photoStorageService.load("../outside.txt"));
            assertThrows(IllegalArgumentException.class, () -> photoStorageService.load("sub/../../outside.txt"));
        }

        @Test
        void billingDocumentStorageRefusesNamesOutsideUploads() {
            assertThrows(IllegalArgumentException.class, () -> billPaymentService.load("../outside.txt"));
        }

        @Test
        void encodedDotDotIsNotServed() throws Exception {
            mockMvc.perform(get("/api/bills/..%2F..%2Fbuild.gradle").header("Authorization", bearer("owner")))
                    .andExpect(status().is4xxClientError());
            mockMvc.perform(get("/api/apartments/photos/..%2F..%2Fbuild.gradle"))
                    .andExpect(status().is4xxClientError());
        }

        @Test
        void normalNamesStillLoad() throws Exception {
            String photo = apartmentPhoto(aptA);
            mockMvc.perform(get("/api/apartments/photos/" + photo))
                    .andExpect(status().isOk());
        }
    }

    // ---- 08: apartment photo route serves only apartment photos ----

    @Nested
    class ApartmentPhotoRoute {

        @Test
        void apartmentPhotoWithoutLogin_returns200() throws Exception {
            String photo = apartmentPhoto(aptA);
            mockMvc.perform(get("/api/apartments/photos/" + photo))
                    .andExpect(status().isOk());
        }

        @Test
        void apartmentDocumentNameViaPhotoRoute_returns404() throws Exception {
            HandoverProtocol protocol = apartmentDocument(aptA);
            mockMvc.perform(get("/api/apartments/photos/" + protocol.getFileName()))
                    .andExpect(status().isNotFound());
        }

        @Test
        void applicantDocumentNameViaPhotoRoute_returns404() throws Exception {
            String name = applicantDocument(aptA);
            mockMvc.perform(get("/api/apartments/photos/" + name))
                    .andExpect(status().isNotFound());
        }
    }

    // ---- 09: single inspection photo download removed ----

    @Nested
    class InspectionPhotoDownload {

        @Test
        void singleInspectionPhotoDownload_isGone() throws Exception {
            String name = storeFile("inspection.png");
            mockMvc.perform(get("/api/inspections/photos/" + name).header("Authorization", bearer("owner")))
                    .andExpect(status().is4xxClientError());
            mockMvc.perform(get("/api/inspections/photos/" + name))
                    .andExpect(status().is4xxClientError());
        }
    }

    // ---- 10: apartment documents ----

    @Nested
    class ApartmentDocumentDownload {

        @Test
        void withoutLogin_isRejected() throws Exception {
            HandoverProtocol protocol = apartmentDocument(aptA);
            mockMvc.perform(get("/api/apartments/protocols/" + protocol.getFileName()))
                    .andExpect(status().isForbidden());
        }

        @Test
        void tenantDownloadsDocumentOfOtherApartment_returns403() throws Exception {
            HandoverProtocol protocol = apartmentDocument(aptB);
            mockMvc.perform(get("/api/apartments/protocols/" + protocol.getFileName())
                            .header("Authorization", bearer("tenant")))
                    .andExpect(status().isForbidden());
        }

        @Test
        void tenantDownloadsDocumentOfAssignedApartment_returns200() throws Exception {
            HandoverProtocol protocol = apartmentDocument(aptA);
            mockMvc.perform(get("/api/apartments/protocols/" + protocol.getFileName())
                            .header("Authorization", bearer("tenant")))
                    .andExpect(status().isOk());
        }

        @Test
        void ownerAndAdminDownloadAnyDocument_return200() throws Exception {
            HandoverProtocol protocol = apartmentDocument(aptB);
            mockMvc.perform(get("/api/apartments/protocols/" + protocol.getFileName())
                            .header("Authorization", bearer("owner")))
                    .andExpect(status().isOk());
            mockMvc.perform(get("/api/apartments/protocols/" + protocol.getFileName())
                            .header("Authorization", bearer("admin")))
                    .andExpect(status().isOk());
        }

        @Test
        void nameThatIsNotAnApartmentDocument_returns404() throws Exception {
            String photo = apartmentPhoto(aptA);
            mockMvc.perform(get("/api/apartments/protocols/" + photo).header("Authorization", bearer("owner")))
                    .andExpect(status().isNotFound());
        }
    }

    // ---- 11: ticket photos ----

    @Nested
    class TicketPhotoDownload {

        @Test
        void withoutLogin_isRejected() throws Exception {
            String photo = ticketPhoto(aptA, "tenant");
            mockMvc.perform(get("/api/tickets/photos/" + photo))
                    .andExpect(status().isForbidden());
        }

        @Test
        void tenantFetchesPhotoOnTicketCreatedBySomeoneElse_returns403() throws Exception {
            String photo = ticketPhoto(aptA, "admin");
            mockMvc.perform(get("/api/tickets/photos/" + photo).header("Authorization", bearer("tenant")))
                    .andExpect(status().isForbidden());
        }

        @Test
        void tenantFetchesPhotoOnOwnTicket_returns200() throws Exception {
            String photo = ticketPhoto(aptA, "tenant");
            mockMvc.perform(get("/api/tickets/photos/" + photo).header("Authorization", bearer("tenant")))
                    .andExpect(status().isOk());
        }

        @Test
        void ownerAndAdminFetchAnyTicketPhoto_return200() throws Exception {
            String photo = ticketPhoto(aptB, "tenant");
            mockMvc.perform(get("/api/tickets/photos/" + photo).header("Authorization", bearer("owner")))
                    .andExpect(status().isOk());
            mockMvc.perform(get("/api/tickets/photos/" + photo).header("Authorization", bearer("admin")))
                    .andExpect(status().isOk());
        }

        @Test
        void nameThatIsNotATicketPhoto_returns404() throws Exception {
            String photo = apartmentPhoto(aptA);
            mockMvc.perform(get("/api/tickets/photos/" + photo).header("Authorization", bearer("owner")))
                    .andExpect(status().isNotFound());
        }
    }
}
