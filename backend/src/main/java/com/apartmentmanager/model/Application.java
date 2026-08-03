package com.apartmentmanager.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "applications")
public class Application {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "apartment_id")
    private Apartment apartment;

    private String applicantName;

    private String applicantEmail;

    private String applicantPhone;

    private String storedFileName;

    private String originalFileName;

    private LocalDateTime submittedAt = LocalDateTime.now();

    @Column(columnDefinition = "TEXT")
    private String formData;

    public Application(Apartment apartment, String applicantName, String applicantEmail,
                       String applicantPhone, String storedFileName, String originalFileName,
                       String formData) {
        this.apartment = apartment;
        this.applicantName = applicantName;
        this.applicantEmail = applicantEmail;
        this.applicantPhone = applicantPhone;
        this.storedFileName = storedFileName;
        this.originalFileName = originalFileName;
        this.formData = formData;
    }
}
