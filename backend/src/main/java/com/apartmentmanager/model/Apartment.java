package com.apartmentmanager.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "apartments")
public class Apartment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Title is required")
    private String title;

    private String description;

    private String location;

    private Double price;

    private Integer rooms;

    private Double area;

    @ElementCollection
    @CollectionTable(name = "apartment_photos", joinColumns = @JoinColumn(name = "apartment_id"))
    @Column(name = "photo_path")
    private List<String> photoPaths = new ArrayList<>();

    @ElementCollection
    @CollectionTable(name = "apartment_metadata", joinColumns = @JoinColumn(name = "apartment_id"))
    @Column(name = "metadata_value")
    private List<String> metadata = new ArrayList<>();

    @OneToMany(mappedBy = "apartment")
    @JsonIgnore
    private List<User> tenants = new ArrayList<>();

    @OneToMany(mappedBy = "apartment", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Contact> contacts = new ArrayList<>();

    @Column(columnDefinition = "TEXT")
    private String presentation;

    @Enumerated(EnumType.STRING)
    private ApartmentStatus status = ApartmentStatus.AVAILABLE_IMMEDIATELY;

    private LocalDate availableFrom;

    @OneToMany(mappedBy = "apartment", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnore
    private List<Note> notes = new ArrayList<>();

    private LocalDateTime createdAt = LocalDateTime.now();

    @JsonProperty("tenant")
    public String getTenant() {
        if (tenants == null || tenants.isEmpty()) return null;
        return tenants.stream()
                .filter(u -> u.getRole() == Role.TENANT)
                .map(User::getUsername)
                .findFirst()
                .orElse(null);
    }

    public Apartment(String title, String description, String location, Double price, Integer rooms, Double area) {
        this.title = title;
        this.description = description;
        this.location = location;
        this.price = price;
        this.rooms = rooms;
        this.area = area;
    }
}
