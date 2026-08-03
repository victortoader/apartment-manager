package com.apartmentmanager.controller;

import com.apartmentmanager.audit.Audited;
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
import lombok.RequiredArgsConstructor;
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
@RequiredArgsConstructor
public class ApartmentController {

    private final ApartmentService apartmentService;
    private final PhotoStorageService photoStorageService;
    private final HandoverProtocolService protocolService;
    private final UserRepository userRepository;
    private final BillPaymentRepository billPaymentRepository;
    private final TicketRepository ticketRepository;

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

    @Audited(action = "APARTMENT_CREATED", message = "Created apartment #{result.id}: {result.title}")
    @PostMapping
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN')")
    public Apartment create(@Valid @RequestBody Apartment apartment) {
        Apartment saved = apartmentService.save(apartment);
        return saved;
    }

    @Audited(action = "APARTMENT_DELETED", message = "Deleted apartment #{id}")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('OWNER')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        apartmentService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @Audited(action = "APARTMENT_PHOTO_UPLOADED", message = "Uploaded photo to apartment #{id}")
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

    @Audited(action = "PROTOCOL_UPLOADED", message = "Uploaded protocol ({documentType}) to apartment #{id}")
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
                HandoverProtocol protocol = protocolService.findByFileName(fileName);
                String originalName = (protocol != null ? protocol.getOriginalName() : fileName);
                return ResponseEntity.ok()
                        .contentType(MediaType.parseMediaType(contentType))
                        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + originalName + "\"")
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
            apartment.getPhotoPaths() != null ? apartment.getPhotoPaths() : List.of(),
            apartment.getStatus(),
            apartment.getAvailableFrom()
        );
        return ResponseEntity.ok(dto);
    }

    @GetMapping("/presentation-list")
    public ResponseEntity<List<PresentationDto>> getPresentationList() {
        List<Apartment> apartments = apartmentService.findAll();
        List<PresentationDto> dtos = apartments.stream().map(apt -> new PresentationDto(
            apt.getId(),
            apt.getTitle(),
            apt.getLocation(),
            apt.getPrice(),
            apt.getRooms(),
            apt.getArea(),
            apt.getDescription(),
            apt.getPresentation() != null ? apt.getPresentation() : "",
            apt.getPhotoPaths() != null ? apt.getPhotoPaths() : List.of(),
            apt.getStatus(),
            apt.getAvailableFrom()
        )).toList();
        return ResponseEntity.ok(dtos);
    }

    @Audited(action = "PRESENTATION_UPDATED", message = "Updated presentation for apartment #{id}")
    @PutMapping("/{id}/presentation")
    @PreAuthorize("hasRole('OWNER')")
    public ResponseEntity<String> updatePresentation(@PathVariable Long id, @RequestBody String content) {
        Apartment apartment = apartmentService.findById(id);
        apartment.setPresentation(content);
        apartmentService.save(apartment);
        return ResponseEntity.ok(content);
    }

    @Audited(action = "APARTMENT_DETAILS_UPDATED", message = "Updated details for apartment #{id}")
    @PutMapping("/{id}/details")
    @PreAuthorize("hasRole('OWNER')")
    public ResponseEntity<Apartment> updateDetails(@PathVariable Long id, @RequestBody java.util.Map<String, Object> body) {
        Apartment apartment = apartmentService.findById(id);
        if (body.containsKey("price")) {
            apartment.setPrice(body.get("price") != null ? Double.parseDouble(body.get("price").toString()) : null);
        }
        if (body.containsKey("description")) {
            apartment.setDescription((String) body.get("description"));
        }
        if (body.containsKey("status")) {
            apartment.setStatus(com.apartmentmanager.model.ApartmentStatus.valueOf((String) body.get("status")));
        }
        if (body.containsKey("availableFrom")) {
            String val = (String) body.get("availableFrom");
            apartment.setAvailableFrom(val != null && !val.isEmpty() ? java.time.LocalDate.parse(val) : null);
        }
        apartmentService.save(apartment);
        return ResponseEntity.ok(apartment);
    }

    @Audited(action = "APARTMENT_METADATA_UPDATED", message = "Updated metadata for apartment #{id}")
    @PutMapping("/{id}/metadata")
    @PreAuthorize("hasRole('OWNER')")
    public ResponseEntity<Apartment> updateMetadata(@PathVariable Long id,
            @RequestBody List<String> metadata) {
        Apartment apartment = apartmentService.findById(id);
        apartment.setMetadata(new java.util.ArrayList<>(metadata));
        apartmentService.save(apartment);
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
