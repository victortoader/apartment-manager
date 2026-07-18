package com.apartmentmanager.controller;

import com.apartmentmanager.model.Apartment;
import com.apartmentmanager.model.BillPayment;
import com.apartmentmanager.model.DocumentType;
import com.apartmentmanager.model.HandoverProtocol;
import com.apartmentmanager.model.User;
import com.apartmentmanager.repository.BillPaymentRepository;
import com.apartmentmanager.repository.TicketRepository;
import com.apartmentmanager.repository.UserRepository;
import com.apartmentmanager.service.ApartmentService;
import com.apartmentmanager.service.HandoverProtocolService;
import com.apartmentmanager.service.PhotoStorageService;
import com.apartmentmanager.service.AuditService;
import com.apartmentmanager.model.TicketStatus;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import jakarta.servlet.http.HttpServletRequest;

import jakarta.validation.Valid;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

@RestController
@RequestMapping("/api/apartments")
public class ApartmentController {

    private final ApartmentService apartmentService;
    private final PhotoStorageService photoStorageService;
    private final HandoverProtocolService protocolService;
    private final UserRepository userRepository;
    private final BillPaymentRepository billPaymentRepository;
    private final TicketRepository ticketRepository;
    private final AuditService auditService;

    public ApartmentController(ApartmentService apartmentService,
                               PhotoStorageService photoStorageService,
                               HandoverProtocolService protocolService,
                               UserRepository userRepository,
                               BillPaymentRepository billPaymentRepository,
                               TicketRepository ticketRepository,
                               AuditService auditService) {
        this.apartmentService = apartmentService;
        this.photoStorageService = photoStorageService;
        this.protocolService = protocolService;
        this.userRepository = userRepository;
        this.billPaymentRepository = billPaymentRepository;
        this.ticketRepository = ticketRepository;
        this.auditService = auditService;
    }

    @GetMapping
    public List<Apartment> getAll(Authentication auth) {
        User user = userRepository.findByUsername(auth.getName()).orElseThrow();
        if (user.getRole() == com.apartmentmanager.model.Role.TENANT) {
            if (user.getApartment() == null) {
                return List.of();
            }
            return List.of(apartmentService.findById(user.getApartment().getId()));
        }
        return apartmentService.findAll();
    }

    @GetMapping("/summary")
    public List<ApartmentSummaryDto> getSummary(Authentication auth) {
        User user = userRepository.findByUsername(auth.getName()).orElseThrow();
        List<Apartment> apts;
        if (user.getRole() == com.apartmentmanager.model.Role.TENANT) {
            if (user.getApartment() == null) return List.of();
            apts = List.of(apartmentService.findById(user.getApartment().getId()));
        } else {
            apts = apartmentService.findAll();
        }

        return apts.stream().map(apt -> {
            List<BillPayment> bills = billPaymentRepository.findByApartmentIdOrderByUploadDateDesc(apt.getId());
            List<ApartmentSummaryDto.BillSummary> recentBills = bills.stream()
                    .limit(5)
                    .map(b -> new ApartmentSummaryDto.BillSummary(
                            b.getId(), b.getOriginalFileName(), b.getStoredFileName(), b.getBillType(), b.getUploadDate()))
                    .toList();

            long openTickets = ticketRepository.countByApartmentIdAndStatus(apt.getId(), com.apartmentmanager.model.TicketStatus.NEW);

            return new ApartmentSummaryDto(
                    apt.getId(), apt.getTitle(), apt.getLocation(), apt.getPrice(),
                    apt.getRooms(), apt.getArea(), apt.getTenant(),
                    apt.getPhotoPaths() != null ? apt.getPhotoPaths() : List.of(),
                    recentBills, (int) openTickets);
        }).toList();
    }

    @GetMapping("/{id}")
    public Apartment getById(@PathVariable Long id, Authentication auth) {
        Apartment apartment = apartmentService.findById(id);
        User user = userRepository.findByUsername(auth.getName()).orElseThrow();
        if (user.getRole() == com.apartmentmanager.model.Role.TENANT) {
            if (user.getApartment() == null || !user.getApartment().getId().equals(id)) {
                throw new RuntimeException("Access denied");
            }
        }
        return apartment;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN')")
    public Apartment create(@Valid @RequestBody Apartment apartment, Authentication auth) {
        User user = userRepository.findByUsername(auth.getName()).orElseThrow();
        Apartment saved = apartmentService.save(apartment);
        auditService.log(user.getUsername(), user.getRole().name(), "APARTMENT_CREATED",
                "Created apartment #" + saved.getId() + ": " + saved.getTitle(), null);
        return saved;
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('OWNER')")
    public ResponseEntity<Void> delete(@PathVariable Long id, Authentication auth) {
        String actorName = auth.getName();
        User actor = userRepository.findByUsername(actorName).orElseThrow();
        String actorRole = actor.getRole().name();
        apartmentService.delete(id);
        auditService.log(actorName, actorRole, "APARTMENT_DELETED",
                "Deleted apartment #" + id, null);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/photos")
    public ResponseEntity<Apartment> uploadPhoto(@PathVariable Long id,
                                                  @RequestParam("file") MultipartFile file,
                                                  Authentication auth) {
        User user = userRepository.findByUsername(auth.getName()).orElseThrow();
        if (user.getRole() == com.apartmentmanager.model.Role.TENANT) {
            if (user.getApartment() == null || !user.getApartment().getId().equals(id)) {
                return ResponseEntity.status(403).build();
            }
        }

        try {
            String fileName = photoStorageService.store(file);
            Apartment apartment = apartmentService.findById(id);
            apartment.getPhotoPaths().add(fileName);
            apartmentService.save(apartment);
            auditService.log(user.getUsername(), user.getRole().name(), "APARTMENT_PHOTO_UPLOADED",
                    "Uploaded photo to apartment #" + id, null);
            return ResponseEntity.ok(apartment);
        } catch (IOException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/photos/{fileName}")
    public ResponseEntity<Resource> getPhoto(@PathVariable String fileName) {
        try {
            Path filePath = photoStorageService.load(fileName);
            Resource resource = new UrlResource(filePath.toUri());
            if (resource.exists()) {
                return ResponseEntity.ok()
                        .contentType(MediaType.IMAGE_JPEG)
                        .header(HttpHeaders.CONTENT_DISPOSITION, "inline")
                        .body(resource);
            }
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/{id}/protocols")
    public List<HandoverProtocol> getProtocols(@PathVariable Long id, Authentication auth) {
        User user = userRepository.findByUsername(auth.getName()).orElseThrow();
        if (user.getRole() == com.apartmentmanager.model.Role.TENANT) {
            if (user.getApartment() == null || !user.getApartment().getId().equals(id)) {
                return List.of();
            }
        }
        return protocolService.findByApartmentId(id);
    }

    @PostMapping("/{id}/protocols")
    public ResponseEntity<HandoverProtocol> uploadProtocol(@PathVariable Long id,
                                                           @RequestParam("file") MultipartFile file,
                                                           @RequestParam("documentType") DocumentType documentType,
                                                           Authentication auth) {
        User user = userRepository.findByUsername(auth.getName()).orElseThrow();
        if (user.getRole() == com.apartmentmanager.model.Role.TENANT) {
            if (user.getApartment() == null || !user.getApartment().getId().equals(id)) {
                return ResponseEntity.status(403).build();
            }
        }

        try {
            Apartment apartment = apartmentService.findById(id);
            HandoverProtocol protocol = protocolService.upload(id, file, documentType, apartment);
            auditService.log(user.getUsername(), user.getRole().name(), "PROTOCOL_UPLOADED",
                    "Uploaded protocol (" + documentType + ") to apartment #" + id, null);
            return ResponseEntity.ok(protocol);
        } catch (IOException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/protocols/{fileName}")
    public ResponseEntity<Resource> getProtocolFile(@PathVariable String fileName) {
        try {
            Path filePath = protocolService.loadFile(fileName);
            Resource resource = new UrlResource(filePath.toUri());
            if (resource.exists()) {
                String contentType = determineContentType(fileName);
                return ResponseEntity.ok()
                        .contentType(MediaType.parseMediaType(contentType))
                        .header(HttpHeaders.CONTENT_DISPOSITION, "inline")
                        .body(resource);
            }
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/protocols/{id}")
    @PreAuthorize("hasRole('OWNER')")
    public ResponseEntity<Void> deleteProtocol(@PathVariable Long id) {
        protocolService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/presentation")
    public ResponseEntity<PresentationDto> getPresentation(@PathVariable Long id) {
        Apartment apartment = apartmentService.findById(id);
        PresentationDto dto = new PresentationDto(
            apartment.getId(),
            apartment.getTitle(),
            apartment.getLocation(),
            apartment.getPrice(),
            apartment.getRooms(),
            apartment.getArea(),
            apartment.getDescription(),
            apartment.getPresentation() != null ? apartment.getPresentation() : "",
            apartment.getPhotoPaths() != null ? apartment.getPhotoPaths() : List.of()
        );
        return ResponseEntity.ok(dto);
    }

    @PutMapping("/{id}/presentation")
    @PreAuthorize("hasRole('OWNER')")
    public ResponseEntity<String> updatePresentation(@PathVariable Long id, @RequestBody String content, Authentication auth) {
        Apartment apartment = apartmentService.findById(id);
        apartment.setPresentation(content);
        apartmentService.save(apartment);
        User user = userRepository.findByUsername(auth.getName()).orElseThrow();
        auditService.log(user.getUsername(), user.getRole().name(), "PRESENTATION_UPDATED",
                "Updated presentation for apartment #" + id, null);
        return ResponseEntity.ok(content);
    }

    @PutMapping("/{id}/details")
    @PreAuthorize("hasRole('OWNER')")
    public ResponseEntity<Apartment> updateDetails(@PathVariable Long id, @RequestBody java.util.Map<String, Object> body, Authentication auth) {
        Apartment apartment = apartmentService.findById(id);
        if (body.containsKey("price")) {
            apartment.setPrice(body.get("price") != null ? Double.parseDouble(body.get("price").toString()) : null);
        }
        if (body.containsKey("description")) {
            apartment.setDescription((String) body.get("description"));
        }
        apartmentService.save(apartment);
        User user = userRepository.findByUsername(auth.getName()).orElseThrow();
        auditService.log(user.getUsername(), user.getRole().name(), "APARTMENT_DETAILS_UPDATED",
                "Updated details for apartment #" + id, null);
        return ResponseEntity.ok(apartment);
    }

    private String determineContentType(String fileName) {
        if (fileName.endsWith(".pdf")) return "application/pdf";
        if (fileName.endsWith(".doc") || fileName.endsWith(".docx")) return "application/msword";
        if (fileName.endsWith(".jpg") || fileName.endsWith(".jpeg")) return "image/jpeg";
        if (fileName.endsWith(".png")) return "image/png";
        return "application/octet-stream";
    }
}
