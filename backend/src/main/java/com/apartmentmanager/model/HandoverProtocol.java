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
@Table(name = "handover_protocols")
public class HandoverProtocol {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String fileName;

    private String originalName;

    private String contentType;

    @Enumerated(EnumType.STRING)
    private DocumentType documentType;

    private LocalDateTime createdAt = LocalDateTime.now();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "apartment_id")
    @JsonIgnore
    private Apartment apartment;

    public HandoverProtocol(String fileName, String originalName, String contentType, DocumentType documentType, Apartment apartment) {
        this.fileName = fileName;
        this.originalName = originalName;
        this.contentType = contentType;
        this.documentType = documentType;
        this.apartment = apartment;
    }
}
