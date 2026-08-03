package com.apartmentmanager.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "bill_payments")
public class BillPayment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String originalFileName;

    private String storedFileName;

    private String contentType;

    private String billType;

    private String documentType;

    private Double extractedAmount;

    private String extractedCurrency;

    private Double ocrConfidence;

    private Boolean ocrFailed = false;

    private LocalDateTime uploadDate = LocalDateTime.now();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "apartment_id")
    @JsonIgnore
    private Apartment apartment;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "uploaded_by_id")
    private User uploadedBy;

    public BillPayment(String originalFileName, String storedFileName, String contentType, String billType, String documentType, Apartment apartment, User uploadedBy) {
        this.originalFileName = originalFileName;
        this.storedFileName = storedFileName;
        this.contentType = contentType;
        this.billType = billType;
        this.documentType = documentType;
        this.apartment = apartment;
        this.uploadedBy = uploadedBy;
    }
}
