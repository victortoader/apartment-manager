package com.apartmentmanager;

import com.apartmentmanager.model.Apartment;
import com.apartmentmanager.model.User;
import com.apartmentmanager.service.ContactService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ContactTest extends AbstractIntegrationTest {

    @Autowired private ContactService contactService;

    private Apartment aptA;
    private Apartment aptB;

    @BeforeEach
    void setUp() {
        aptA = createApartment("Contacts Apt A");
        aptB = createApartment("Contacts Apt B");
        contactService.create(aptA.getId(), "Plumber A", "0700 000 001");
        contactService.create(aptB.getId(), "Plumber B", "0700 000 002");
    }

    private void assignTenantTo(Apartment apt) {
        User tenant = userRepository.findByUsername("tenant").orElseThrow();
        tenant.setApartment(apt);
        userRepository.save(tenant);
        entityManager.flush();
        entityManager.clear();
    }

    @Test
    void tenantGetsContactsOfOtherApartment_returns403() throws Exception {
        assignTenantTo(aptA);

        mockMvc.perform(get("/api/apartments/" + aptB.getId() + "/contacts")
                        .header("Authorization", bearer("tenant")))
                .andExpect(status().isForbidden());
    }

    @Test
    void tenantGetsContactsOfAssignedApartment_returns200() throws Exception {
        assignTenantTo(aptA);

        mockMvc.perform(get("/api/apartments/" + aptA.getId() + "/contacts")
                        .header("Authorization", bearer("tenant")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].name").value("Plumber A"));
    }

    @Test
    void tenantWithoutAssignedApartment_returns403() throws Exception {
        assignTenantTo(null);

        mockMvc.perform(get("/api/apartments/" + aptA.getId() + "/contacts")
                        .header("Authorization", bearer("tenant")))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminGetsContactsOfAnyApartment_returns200() throws Exception {
        mockMvc.perform(get("/api/apartments/" + aptB.getId() + "/contacts")
                        .header("Authorization", bearer("admin")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Plumber B"));
    }

    @Test
    void ownerGetsContactsOfAnyApartment_returns200() throws Exception {
        mockMvc.perform(get("/api/apartments/" + aptB.getId() + "/contacts")
                        .header("Authorization", bearer("owner")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Plumber B"));
    }
}
