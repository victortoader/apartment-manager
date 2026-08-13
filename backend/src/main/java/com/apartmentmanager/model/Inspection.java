package com.apartmentmanager.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
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

    private String firstName;

    private String previousName;

    private String previousFirstName;

    private String previousAddress;

    private String previousPostalCode;

    private String previousCity;

    private String previousPhone;

    private String previousEmail;

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
}
