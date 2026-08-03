package com.apartmentmanager;

import com.apartmentmanager.model.Apartment;
import com.apartmentmanager.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class ApartmentTest extends AbstractIntegrationTest {

    @Nested
    class CRUD {

        private Apartment apt1;
        private Apartment apt2;

        @BeforeEach
        void setUp() {
            apt1 = createApartment("Apt One");
            apt2 = createApartment("Apt Two");
        }

        @Test
        void getAllAsOwner_returnsAllApartments() throws Exception {
            mockMvc.perform(get("/api/apartments").header("Authorization", bearer("owner")))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(2)));
        }

        @Test
        void getAllAsAdmin_returnsAllApartments() throws Exception {
            mockMvc.perform(get("/api/apartments").header("Authorization", bearer("admin")))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(2)));
        }

        @Test
        void getAllAsTenantWithApartment_returnsOnlyAssigned() throws Exception {
            User tenant = userRepository.findByUsername("tenant").orElseThrow();
            tenant.setApartment(apt1);
            userRepository.save(tenant);

            mockMvc.perform(get("/api/apartments").header("Authorization", bearer("tenant")))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(1)))
                    .andExpect(jsonPath("$[0].id").value(apt1.getId()));
        }

        @Test
        void getAllAsTenantWithoutApartment_returnsEmpty() throws Exception {
            mockMvc.perform(get("/api/apartments").header("Authorization", bearer("tenant")))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(0)));
        }

        @Test
        void getByIdAsOwner_returnsApartment() throws Exception {
            mockMvc.perform(get("/api/apartments/" + apt1.getId())
                            .header("Authorization", bearer("owner")))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.title").value("Apt One"))
                    .andExpect(jsonPath("$.location").value("Location"))
                    .andExpect(jsonPath("$.price").value(1000.0))
                    .andExpect(jsonPath("$.rooms").value(2))
                    .andExpect(jsonPath("$.area").value(50.0));
        }

        @Test
        void getByIdAsTenantOwnApartment_returnsApartment() throws Exception {
            User tenant = userRepository.findByUsername("tenant").orElseThrow();
            tenant.setApartment(apt1);
            userRepository.save(tenant);

            mockMvc.perform(get("/api/apartments/" + apt1.getId())
                            .header("Authorization", bearer("tenant")))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.title").value("Apt One"));
        }

        @Test
        void getByIdAsTenantOtherApartment_throwsAccessDenied() throws Exception {
            User tenant = userRepository.findByUsername("tenant").orElseThrow();
            tenant.setApartment(apt1);
            userRepository.save(tenant);
            entityManager.flush();
            entityManager.clear();

            mockMvc.perform(get("/api/apartments/" + apt2.getId())
                            .header("Authorization", bearer("tenant")))
                    .andExpect(status().isInternalServerError());
        }

        @Test
        void getByIdNonExistent_returnsError() throws Exception {
            mockMvc.perform(get("/api/apartments/99999")
                            .header("Authorization", bearer("owner")))
                    .andExpect(status().isInternalServerError());
        }

        @Test
        void createApartmentAsOwner_succeeds() throws Exception {
            mockMvc.perform(post("/api/apartments")
                            .header("Authorization", bearer("owner"))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"title\":\"New Apt\",\"description\":\"Nice\",\"location\":\"Bucharest\",\"price\":800,\"rooms\":1,\"area\":30}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.title").value("New Apt"))
                    .andExpect(jsonPath("$.description").value("Nice"))
                    .andExpect(jsonPath("$.location").value("Bucharest"));

            assert apartmentRepository.findAll().stream().anyMatch(a -> a.getTitle().equals("New Apt"));
        }

        @Test
        void createApartmentAsAdmin_succeeds() throws Exception {
            mockMvc.perform(post("/api/apartments")
                            .header("Authorization", bearer("admin"))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"title\":\"Admin Apt\",\"price\":600,\"rooms\":1,\"area\":25}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.title").value("Admin Apt"));
        }

        @Test
        void createApartmentAsTenant_returns403() throws Exception {
            mockMvc.perform(post("/api/apartments")
                            .header("Authorization", bearer("tenant"))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"title\":\"Tenant Apt\",\"price\":500}"))
                    .andExpect(status().isForbidden());
        }

        @Test
        void createApartmentMissingTitle_returns400() throws Exception {
            mockMvc.perform(post("/api/apartments")
                            .header("Authorization", bearer("owner"))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"price\":500,\"rooms\":1,\"area\":30}"))
                    .andExpect(status().isBadRequest());
        }

        @Test
        void deleteApartmentAsOwner_succeeds() throws Exception {
            mockMvc.perform(delete("/api/apartments/" + apt1.getId())
                            .header("Authorization", bearer("owner")))
                    .andExpect(status().isNoContent());

            assert apartmentRepository.findById(apt1.getId()).isEmpty();
        }

        @Test
        void deleteApartmentAsAdmin_returns403() throws Exception {
            mockMvc.perform(delete("/api/apartments/" + apt1.getId())
                            .header("Authorization", bearer("admin")))
                    .andExpect(status().isForbidden());
        }

        @Test
        void deleteApartmentAsTenant_returns403() throws Exception {
            mockMvc.perform(delete("/api/apartments/" + apt1.getId())
                            .header("Authorization", bearer("tenant")))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    class TenantField {

        @Test
        void apartmentWithTenant_showsTenantName() throws Exception {
            Apartment apt = createApartment("Tenant Field Apt");
            User tenant = userRepository.findByUsername("tenant").orElseThrow();
            tenant.setApartment(apt);
            userRepository.save(tenant);
            entityManager.flush();
            entityManager.clear();

            mockMvc.perform(get("/api/apartments").header("Authorization", bearer("owner")))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[?(@.title=='Tenant Field Apt')].tenant").value("tenant"));
        }

        @Test
        void apartmentWithoutTenant_tenantIsNull() throws Exception {
            createApartment("No Tenant Apt");
            entityManager.flush();
            entityManager.clear();

            mockMvc.perform(get("/api/apartments").header("Authorization", bearer("owner")))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[?(@.title=='No Tenant Apt')].tenant").value(contains((Object) null)));
        }

        @Test
        void tenantAssignment_reflectedInApartmentList() throws Exception {
            Apartment apt = createApartment("Assignment Test Apt");
            User tenant = userRepository.findByUsername("tenant").orElseThrow();
            entityManager.flush();
            entityManager.clear();

            mockMvc.perform(get("/api/apartments").header("Authorization", bearer("owner")))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[?(@.title=='Assignment Test Apt')].tenant").value(contains((Object) null)));

            tenant.setApartment(apt);
            userRepository.save(tenant);
            entityManager.flush();
            entityManager.clear();

            mockMvc.perform(get("/api/apartments").header("Authorization", bearer("owner")))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[?(@.title=='Assignment Test Apt')].tenant").value("tenant"));
        }

        @Test
        void reassignTenant_updatesApartmentField() throws Exception {
            Apartment apt1 = createApartment("Apt A");
            Apartment apt2 = createApartment("Apt B");
            User tenant = userRepository.findByUsername("tenant").orElseThrow();

            tenant.setApartment(apt1);
            userRepository.save(tenant);
            entityManager.flush();
            entityManager.clear();

            mockMvc.perform(get("/api/apartments/" + apt1.getId())
                            .header("Authorization", bearer("owner")))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.tenant").value("tenant"));

            mockMvc.perform(get("/api/apartments/" + apt2.getId())
                            .header("Authorization", bearer("owner")))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.tenant").value(nullValue()));

            tenant.setApartment(apt2);
            userRepository.save(tenant);
            entityManager.flush();
            entityManager.clear();

            mockMvc.perform(get("/api/apartments/" + apt1.getId())
                            .header("Authorization", bearer("owner")))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.tenant").value(nullValue()));

            mockMvc.perform(get("/api/apartments/" + apt2.getId())
                            .header("Authorization", bearer("owner")))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.tenant").value("tenant"));
        }
    }
}
