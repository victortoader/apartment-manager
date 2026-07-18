package com.example.demo.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

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

    @OneToMany(mappedBy = "apartment")
    @JsonIgnore
    private List<User> tenants = new ArrayList<>();

    @OneToMany(mappedBy = "apartment", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Contact> contacts = new ArrayList<>();

    @Column(columnDefinition = "TEXT")
    private String presentation;

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

    public Apartment() {}

    public Apartment(String title, String description, String location, Double price, Integer rooms, Double area) {
        this.title = title;
        this.description = description;
        this.location = location;
        this.price = price;
        this.rooms = rooms;
        this.area = area;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }

    public Double getPrice() { return price; }
    public void setPrice(Double price) { this.price = price; }

    public Integer getRooms() { return rooms; }
    public void setRooms(Integer rooms) { this.rooms = rooms; }

    public Double getArea() { return area; }
    public void setArea(Double area) { this.area = area; }

    public List<User> getTenants() { return tenants; }
    public void setTenants(List<User> tenants) { this.tenants = tenants; }

    public List<Contact> getContacts() { return contacts; }
    public void setContacts(List<Contact> contacts) { this.contacts = contacts; }

    public List<String> getPhotoPaths() { return photoPaths; }
    public void setPhotoPaths(List<String> photoPaths) { this.photoPaths = photoPaths; }

    public String getPresentation() { return presentation; }
    public void setPresentation(String presentation) { this.presentation = presentation; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
