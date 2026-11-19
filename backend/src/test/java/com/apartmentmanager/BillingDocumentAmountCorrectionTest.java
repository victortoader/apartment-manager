package com.apartmentmanager;

import com.apartmentmanager.model.Apartment;
import com.apartmentmanager.model.BillPayment;
import com.apartmentmanager.model.User;
import com.apartmentmanager.repository.BillPaymentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class BillingDocumentAmountCorrectionTest extends AbstractIntegrationTest {

    @Autowired private BillPaymentRepository billPaymentRepository;

    private BillPayment billA;
    private BillPayment billB;

    @BeforeEach
    void setUp() {
        Apartment aptA = createApartment("Amount Apt A");
        Apartment aptB = createApartment("Amount Apt B");
        billA = saveBill(aptA, "a.pdf");
        billB = saveBill(aptB, "b.pdf");

        User tenant = userRepository.findByUsername("tenant").orElseThrow();
        tenant.setApartment(aptA);
        userRepository.save(tenant);
        entityManager.flush();
        entityManager.clear();
    }

    private BillPayment saveBill(Apartment apt, String storedFileName) {
        BillPayment bill = new BillPayment("original.pdf", storedFileName, "application/pdf",
                "Electricity", "bill", apt, null);
        bill.setExtractedAmount(50.0);
        return billPaymentRepository.save(bill);
    }

    private Double storedAmount(BillPayment bill) {
        entityManager.flush();
        entityManager.clear();
        return billPaymentRepository.findById(bill.getId()).orElseThrow().getExtractedAmount();
    }

    @Test
    void tenantCorrectsAmountOnOtherApartment_returns403AndAmountUnchanged() throws Exception {
        mockMvc.perform(put("/api/bills/" + billB.getId() + "/amount")
                        .header("Authorization", bearer("tenant"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amount\": 999.0}"))
                .andExpect(status().isForbidden());

        assertThat(storedAmount(billB)).isEqualTo(50.0);
    }

    @Test
    void tenantCorrectsAmountOnAssignedApartment_returns200AndAmountUpdated() throws Exception {
        mockMvc.perform(put("/api/bills/" + billA.getId() + "/amount")
                        .header("Authorization", bearer("tenant"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amount\": 123.45}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.extractedAmount").value(123.45));

        assertThat(storedAmount(billA)).isEqualTo(123.45);
    }

    @Test
    void tenantWithoutAssignedApartment_returns403AndAmountUnchanged() throws Exception {
        User tenant = userRepository.findByUsername("tenant").orElseThrow();
        tenant.setApartment(null);
        userRepository.save(tenant);
        entityManager.flush();
        entityManager.clear();

        mockMvc.perform(put("/api/bills/" + billA.getId() + "/amount")
                        .header("Authorization", bearer("tenant"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amount\": 999.0}"))
                .andExpect(status().isForbidden());

        assertThat(storedAmount(billA)).isEqualTo(50.0);
    }

    @Test
    void ownerCorrectsAmountOnAnyApartment_returns200() throws Exception {
        mockMvc.perform(put("/api/bills/" + billB.getId() + "/amount")
                        .header("Authorization", bearer("owner"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amount\": 88.0}"))
                .andExpect(status().isOk());

        assertThat(storedAmount(billB)).isEqualTo(88.0);
    }

    @Test
    void adminCorrectsAmountOnAnyApartment_returns200() throws Exception {
        mockMvc.perform(put("/api/bills/" + billB.getId() + "/amount")
                        .header("Authorization", bearer("admin"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amount\": 77.0}"))
                .andExpect(status().isOk());

        assertThat(storedAmount(billB)).isEqualTo(77.0);
    }
}
