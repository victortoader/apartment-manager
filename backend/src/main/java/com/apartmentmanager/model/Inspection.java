package com.apartmentmanager.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "inspections")
public class Inspection {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "apartment_id")
    @JsonIgnore
    private Apartment apartment;

    private String documentType;

    private LocalDate date;

    private LocalTime time;

    private String companyName;

    private String companyAddress;

    private String companyPostalCode;

    private String companyCity;

    private String companyPhone;

    private String companyEmail;

    private String property;

    private String objectNumber;

    private String rentalObject;

    private String incomingParty;

    @Column(columnDefinition = "TEXT")
    private String confirmationsJson;

    @Column(columnDefinition = "TEXT")
    private String signaturesJson;

    private Long generatedProtocolId;

    private LocalDateTime createdAt = LocalDateTime.now();

    private LocalDateTime updatedAt = LocalDateTime.now();

    @OneToMany(mappedBy = "inspection", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("sortOrder")
    private List<InspectionSection> sections = new ArrayList<>();

    public Inspection() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Apartment getApartment() { return apartment; }
    public void setApartment(Apartment apartment) { this.apartment = apartment; }

    public String getDocumentType() { return documentType; }
    public void setDocumentType(String documentType) { this.documentType = documentType; }

    public LocalDate getDate() { return date; }
    public void setDate(LocalDate date) { this.date = date; }

    public LocalTime getTime() { return time; }
    public void setTime(LocalTime time) { this.time = time; }

    public String getCompanyName() { return companyName; }
    public void setCompanyName(String companyName) { this.companyName = companyName; }

    public String getCompanyAddress() { return companyAddress; }
    public void setCompanyAddress(String companyAddress) { this.companyAddress = companyAddress; }

    public String getCompanyPostalCode() { return companyPostalCode; }
    public void setCompanyPostalCode(String companyPostalCode) { this.companyPostalCode = companyPostalCode; }

    public String getCompanyCity() { return companyCity; }
    public void setCompanyCity(String companyCity) { this.companyCity = companyCity; }

    public String getCompanyPhone() { return companyPhone; }
    public void setCompanyPhone(String companyPhone) { this.companyPhone = companyPhone; }

    public String getCompanyEmail() { return companyEmail; }
    public void setCompanyEmail(String companyEmail) { this.companyEmail = companyEmail; }

    public String getProperty() { return property; }
    public void setProperty(String property) { this.property = property; }

    public String getObjectNumber() { return objectNumber; }
    public void setObjectNumber(String objectNumber) { this.objectNumber = objectNumber; }

    public String getRentalObject() { return rentalObject; }
    public void setRentalObject(String rentalObject) { this.rentalObject = rentalObject; }

    public String getIncomingParty() { return incomingParty; }
    public void setIncomingParty(String incomingParty) { this.incomingParty = incomingParty; }

    public String getConfirmationsJson() { return confirmationsJson; }
    public void setConfirmationsJson(String confirmationsJson) { this.confirmationsJson = confirmationsJson; }

    public String getSignaturesJson() { return signaturesJson; }
    public void setSignaturesJson(String signaturesJson) { this.signaturesJson = signaturesJson; }

    public Long getGeneratedProtocolId() { return generatedProtocolId; }
    public void setGeneratedProtocolId(Long generatedProtocolId) { this.generatedProtocolId = generatedProtocolId; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public List<InspectionSection> getSections() { return sections; }
    public void setSections(List<InspectionSection> sections) { this.sections = sections; }
}
