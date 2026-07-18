package com.apartmentmanager;

import com.apartmentmanager.model.Apartment;
import com.apartmentmanager.model.User;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MvcResult;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class TicketTest extends AbstractIntegrationTest {

    @Nested
    class CreateTicket {

        @Test
        void tenantCreatesTicketForOwnApartment_succeeds() throws Exception {
            Apartment apt = createApartment("My Apt");
            User tenant = userRepository.findByUsername("tenant").orElseThrow();
            tenant.setApartment(apt);
            userRepository.save(tenant);
            entityManager.flush();
            entityManager.clear();

            mockMvc.perform(post("/api/apartments/" + apt.getId() + "/tickets")
                            .header("Authorization", bearer("tenant"))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"title\":\"Leaky faucet\",\"description\":\"Water dripping in kitchen\"}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.title").value("Leaky faucet"))
                    .andExpect(jsonPath("$.description").value("Water dripping in kitchen"))
                    .andExpect(jsonPath("$.status").value("NEW"));
        }

        @Test
        void tenantCreatesTicketForOtherApartment_returns403() throws Exception {
            Apartment apt = createApartment("My Apt");
            Apartment other = createApartment("Other Apt");
            User tenant = userRepository.findByUsername("tenant").orElseThrow();
            tenant.setApartment(apt);
            userRepository.save(tenant);
            entityManager.flush();
            entityManager.clear();

            mockMvc.perform(post("/api/apartments/" + other.getId() + "/tickets")
                            .header("Authorization", bearer("tenant"))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"title\":\"Issue\",\"description\":\"Desc\"}"))
                    .andExpect(status().isForbidden());
        }

        @Test
        void ownerCreatesTicket_succeeds() throws Exception {
            Apartment apt = createApartment("Owner Apt");

            mockMvc.perform(post("/api/apartments/" + apt.getId() + "/tickets")
                            .header("Authorization", bearer("owner"))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"title\":\"Maintenance\",\"description\":\"Check boiler\"}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.title").value("Maintenance"))
                    .andExpect(jsonPath("$.status").value("NEW"));
        }

        @Test
        void adminCreatesTicket_succeeds() throws Exception {
            Apartment apt = createApartment("Admin Apt");

            mockMvc.perform(post("/api/apartments/" + apt.getId() + "/tickets")
                            .header("Authorization", bearer("admin"))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"title\":\"Inspection\",\"description\":\"Annual check\"}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.title").value("Inspection"));
        }

        @Test
        void createTicketMissingTitle_returns400() throws Exception {
            Apartment apt = createApartment("Apt");

            mockMvc.perform(post("/api/apartments/" + apt.getId() + "/tickets")
                            .header("Authorization", bearer("owner"))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"description\":\"No title\"}"))
                    .andExpect(status().isBadRequest());
        }

        @Test
        void unauthenticatedCreateTicket_returns403() throws Exception {
            Apartment apt = createApartment("Apt");

            mockMvc.perform(post("/api/apartments/" + apt.getId() + "/tickets")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"title\":\"X\",\"description\":\"Y\"}"))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    class GetTickets {

        @Test
        void ownerSeesAllTickets() throws Exception {
            Apartment apt = createApartment("Apt");

            mockMvc.perform(post("/api/apartments/" + apt.getId() + "/tickets")
                            .header("Authorization", bearer("owner"))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"title\":\"T1\",\"description\":\"D1\"}"))
                    .andExpect(status().isOk());

            mockMvc.perform(post("/api/apartments/" + apt.getId() + "/tickets")
                            .header("Authorization", bearer("owner"))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"title\":\"T2\",\"description\":\"D2\"}"))
                    .andExpect(status().isOk());

            mockMvc.perform(get("/api/tickets").header("Authorization", bearer("owner")))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(2))));
        }

        @Test
        void adminSeesAllTickets() throws Exception {
            Apartment apt = createApartment("Apt");

            mockMvc.perform(post("/api/apartments/" + apt.getId() + "/tickets")
                            .header("Authorization", bearer("owner"))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"title\":\"T1\",\"description\":\"D1\"}"))
                    .andExpect(status().isOk());

            mockMvc.perform(get("/api/tickets").header("Authorization", bearer("admin")))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(1))));
        }

        @Test
        void tenantCannotSeeAllTickets_returns403() throws Exception {
            mockMvc.perform(get("/api/tickets").header("Authorization", bearer("tenant")))
                    .andExpect(status().isForbidden());
        }

        @Test
        void tenantSeesOnlyOwnTickets() throws Exception {
            Apartment apt = createApartment("My Apt");
            User tenant = userRepository.findByUsername("tenant").orElseThrow();
            tenant.setApartment(apt);
            userRepository.save(tenant);
            entityManager.flush();
            entityManager.clear();

            mockMvc.perform(post("/api/apartments/" + apt.getId() + "/tickets")
                            .header("Authorization", bearer("tenant"))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"title\":\"My Ticket\",\"description\":\"Issue\"}"))
                    .andExpect(status().isOk());

            mockMvc.perform(get("/api/apartments/" + apt.getId() + "/tickets")
                            .header("Authorization", bearer("tenant")))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(1)))
                    .andExpect(jsonPath("$[0].title").value("My Ticket"));
        }

        @Test
        void tenantSeesEmptyForOtherApartment() throws Exception {
            Apartment apt = createApartment("My Apt");
            Apartment other = createApartment("Other Apt");
            User tenant = userRepository.findByUsername("tenant").orElseThrow();
            tenant.setApartment(apt);
            userRepository.save(tenant);
            entityManager.flush();
            entityManager.clear();

            mockMvc.perform(get("/api/apartments/" + other.getId() + "/tickets")
                            .header("Authorization", bearer("tenant")))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(0)));
        }

        @Test
        void getTicketById_asOwner_succeeds() throws Exception {
            Apartment apt = createApartment("Apt");

            MvcResult result = mockMvc.perform(post("/api/apartments/" + apt.getId() + "/tickets")
                            .header("Authorization", bearer("owner"))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"title\":\"Check\",\"description\":\"Desc\"}"))
                    .andExpect(status().isOk())
                    .andReturn();

            Long ticketId = objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asLong();

            mockMvc.perform(get("/api/tickets/" + ticketId)
                            .header("Authorization", bearer("owner")))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.title").value("Check"));
        }

        @Test
        void getTicketById_asCreator_succeeds() throws Exception {
            Apartment apt = createApartment("Apt");
            User tenant = userRepository.findByUsername("tenant").orElseThrow();
            tenant.setApartment(apt);
            userRepository.save(tenant);
            entityManager.flush();
            entityManager.clear();

            MvcResult result = mockMvc.perform(post("/api/apartments/" + apt.getId() + "/tickets")
                            .header("Authorization", bearer("tenant"))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"title\":\"My Issue\",\"description\":\"Desc\"}"))
                    .andExpect(status().isOk())
                    .andReturn();

            Long ticketId = objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asLong();

            mockMvc.perform(get("/api/tickets/" + ticketId)
                            .header("Authorization", bearer("tenant")))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.title").value("My Issue"));
        }
    }

    @Nested
    class UpdateTicketStatus {

        @Test
        void ownerCanUpdateStatus() throws Exception {
            Apartment apt = createApartment("Apt");

            MvcResult result = mockMvc.perform(post("/api/apartments/" + apt.getId() + "/tickets")
                            .header("Authorization", bearer("owner"))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"title\":\"Issue\",\"description\":\"Desc\"}"))
                    .andExpect(status().isOk())
                    .andReturn();

            Long ticketId = objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asLong();

            mockMvc.perform(patch("/api/tickets/" + ticketId)
                            .header("Authorization", bearer("owner"))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"status\":\"IN_PROGRESS\"}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("IN_PROGRESS"));
        }

        @Test
        void adminCanUpdateStatus() throws Exception {
            Apartment apt = createApartment("Apt");

            MvcResult result = mockMvc.perform(post("/api/apartments/" + apt.getId() + "/tickets")
                            .header("Authorization", bearer("owner"))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"title\":\"Issue\",\"description\":\"Desc\"}"))
                    .andExpect(status().isOk())
                    .andReturn();

            Long ticketId = objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asLong();

            mockMvc.perform(patch("/api/tickets/" + ticketId)
                            .header("Authorization", bearer("admin"))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"status\":\"DONE\"}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("DONE"));
        }

        @Test
        void tenantCannotUpdateStatus_returns403() throws Exception {
            Apartment apt = createApartment("Apt");
            User tenant = userRepository.findByUsername("tenant").orElseThrow();
            tenant.setApartment(apt);
            userRepository.save(tenant);
            entityManager.flush();
            entityManager.clear();

            MvcResult result = mockMvc.perform(post("/api/apartments/" + apt.getId() + "/tickets")
                            .header("Authorization", bearer("tenant"))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"title\":\"Issue\",\"description\":\"Desc\"}"))
                    .andExpect(status().isOk())
                    .andReturn();

            Long ticketId = objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asLong();

            mockMvc.perform(patch("/api/tickets/" + ticketId)
                            .header("Authorization", bearer("tenant"))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"status\":\"DONE\"}"))
                    .andExpect(status().isForbidden());
        }

        @Test
        void updateToRejected_succeeds() throws Exception {
            Apartment apt = createApartment("Apt");

            MvcResult result = mockMvc.perform(post("/api/apartments/" + apt.getId() + "/tickets")
                            .header("Authorization", bearer("owner"))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"title\":\"Issue\",\"description\":\"Desc\"}"))
                    .andExpect(status().isOk())
                    .andReturn();

            Long ticketId = objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asLong();

            mockMvc.perform(patch("/api/tickets/" + ticketId)
                            .header("Authorization", bearer("owner"))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"status\":\"REJECTED\"}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("REJECTED"));
        }
    }

    @Nested
    class UnreadTracking {

        @Test
        void newTicketsShowAsUnread() throws Exception {
            Apartment apt = createApartment("Apt");

            mockMvc.perform(post("/api/apartments/" + apt.getId() + "/tickets")
                            .header("Authorization", bearer("owner"))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"title\":\"T1\",\"description\":\"D1\"}"))
                    .andExpect(status().isOk());

            mockMvc.perform(get("/api/tickets/unread/count")
                            .header("Authorization", bearer("owner")))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.count").value(greaterThanOrEqualTo(1)));
        }

        @Test
        void getAllTicketsMarksAsRead() throws Exception {
            Apartment apt = createApartment("Apt");

            mockMvc.perform(post("/api/apartments/" + apt.getId() + "/tickets")
                            .header("Authorization", bearer("owner"))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"title\":\"T1\",\"description\":\"D1\"}"))
                    .andExpect(status().isOk());

            mockMvc.perform(get("/api/tickets").header("Authorization", bearer("owner")))
                    .andExpect(status().isOk());

            mockMvc.perform(get("/api/tickets/unread/count")
                            .header("Authorization", bearer("owner")))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.count").value(0));
        }

        @Test
        void markAsReadEndpoint_works() throws Exception {
            Apartment apt = createApartment("Apt");

            MvcResult result = mockMvc.perform(post("/api/apartments/" + apt.getId() + "/tickets")
                            .header("Authorization", bearer("owner"))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"title\":\"T1\",\"description\":\"D1\"}"))
                    .andExpect(status().isOk())
                    .andReturn();

            Long ticketId = objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asLong();

            mockMvc.perform(post("/api/tickets/" + ticketId + "/read")
                            .header("Authorization", bearer("owner")))
                    .andExpect(status().isOk());

            mockMvc.perform(get("/api/tickets/unread/count")
                            .header("Authorization", bearer("owner")))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.count").value(0));
        }

        @Test
        void getUnreadTickets_returnsOnlyUnread() throws Exception {
            Apartment apt = createApartment("Apt");

            mockMvc.perform(post("/api/apartments/" + apt.getId() + "/tickets")
                            .header("Authorization", bearer("owner"))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"title\":\"Unread\",\"description\":\"D1\"}"))
                    .andExpect(status().isOk());

            mockMvc.perform(get("/api/tickets/unread")
                            .header("Authorization", bearer("owner")))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(1))))
                    .andExpect(jsonPath("$[0].title").value("Unread"));
        }

        @Test
        void differentUsersHaveIndependentReadState() throws Exception {
            Apartment apt = createApartment("Apt");

            mockMvc.perform(post("/api/apartments/" + apt.getId() + "/tickets")
                            .header("Authorization", bearer("owner"))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"title\":\"Shared\",\"description\":\"D1\"}"))
                    .andExpect(status().isOk());

            mockMvc.perform(get("/api/tickets").header("Authorization", bearer("owner")))
                    .andExpect(status().isOk());

            mockMvc.perform(get("/api/tickets/unread/count")
                            .header("Authorization", bearer("owner")))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.count").value(0));

            mockMvc.perform(get("/api/tickets/unread/count")
                            .header("Authorization", bearer("admin")))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.count").value(greaterThanOrEqualTo(1)));
        }

        @Test
        void getTicketByIdMarksAsReadForAdmin() throws Exception {
            Apartment apt = createApartment("Apt");

            MvcResult result = mockMvc.perform(post("/api/apartments/" + apt.getId() + "/tickets")
                            .header("Authorization", bearer("owner"))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"title\":\"T1\",\"description\":\"D1\"}"))
                    .andExpect(status().isOk())
                    .andReturn();

            Long ticketId = objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asLong();

            mockMvc.perform(get("/api/tickets/" + ticketId)
                            .header("Authorization", bearer("admin")))
                    .andExpect(status().isOk());

            mockMvc.perform(get("/api/tickets/unread/count")
                            .header("Authorization", bearer("admin")))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.count").value(0));
        }

        @Test
        void tenantCannotAccessUnreadEndpoint() throws Exception {
            mockMvc.perform(get("/api/tickets/unread/count")
                            .header("Authorization", bearer("tenant")))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    class PhotoLimit {

        private MockMultipartFile fakePhoto(String name) {
            return new MockMultipartFile("file", name, "image/jpeg", new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, 0x00});
        }

        @Test
        void tenantCanUploadUpTo5Photos() throws Exception {
            Apartment apt = createApartment("Apt");
            User tenant = userRepository.findByUsername("tenant").orElseThrow();
            tenant.setApartment(apt);
            userRepository.save(tenant);
            entityManager.flush();
            entityManager.clear();

            MvcResult result = mockMvc.perform(post("/api/apartments/" + apt.getId() + "/tickets")
                            .header("Authorization", bearer("tenant"))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"title\":\"Photo Ticket\",\"description\":\"Desc\"}"))
                    .andExpect(status().isOk())
                    .andReturn();

            Long ticketId = objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asLong();

            for (int i = 1; i <= 5; i++) {
                mockMvc.perform(multipart("/api/tickets/" + ticketId + "/photos")
                                .file(fakePhoto("photo" + i + ".jpg"))
                                .header("Authorization", bearer("tenant")))
                        .andExpect(status().isOk());
            }
        }

        @Test
        void tenantCannotUpload6thPhoto() throws Exception {
            Apartment apt = createApartment("Apt");
            User tenant = userRepository.findByUsername("tenant").orElseThrow();
            tenant.setApartment(apt);
            userRepository.save(tenant);
            entityManager.flush();
            entityManager.clear();

            MvcResult result = mockMvc.perform(post("/api/apartments/" + apt.getId() + "/tickets")
                            .header("Authorization", bearer("tenant"))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"title\":\"Photo Ticket\",\"description\":\"Desc\"}"))
                    .andExpect(status().isOk())
                    .andReturn();

            Long ticketId = objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asLong();

            for (int i = 1; i <= 5; i++) {
                mockMvc.perform(multipart("/api/tickets/" + ticketId + "/photos")
                                .file(fakePhoto("photo" + i + ".jpg"))
                                .header("Authorization", bearer("tenant")))
                        .andExpect(status().isOk());
            }

            mockMvc.perform(multipart("/api/tickets/" + ticketId + "/photos")
                            .file(fakePhoto("photo6.jpg"))
                            .header("Authorization", bearer("tenant")))
                    .andExpect(status().isBadRequest());
        }

        @Test
        void ownerHasNoPhotoLimit() throws Exception {
            Apartment apt = createApartment("Apt");

            MvcResult result = mockMvc.perform(post("/api/apartments/" + apt.getId() + "/tickets")
                            .header("Authorization", bearer("owner"))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"title\":\"Photo Ticket\",\"description\":\"Desc\"}"))
                    .andExpect(status().isOk())
                    .andReturn();

            Long ticketId = objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asLong();

            for (int i = 1; i <= 6; i++) {
                mockMvc.perform(multipart("/api/tickets/" + ticketId + "/photos")
                                .file(fakePhoto("photo" + i + ".jpg"))
                                .header("Authorization", bearer("owner")))
                        .andExpect(status().isOk());
            }
        }
    }
}
