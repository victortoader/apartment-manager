package com.example.demo.controller;

import com.example.demo.model.Role;
import com.example.demo.model.Ticket;
import com.example.demo.model.TicketStatus;
import com.example.demo.model.User;
import com.example.demo.repository.UserRepository;
import com.example.demo.service.PhotoStorageService;
import com.example.demo.service.TicketService;
import com.example.demo.service.AuditService;
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

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class TicketController {

    private final TicketService ticketService;
    private final UserRepository userRepository;
    private final PhotoStorageService photoStorageService;
    private final AuditService auditService;

    public TicketController(TicketService ticketService, UserRepository userRepository,
                            PhotoStorageService photoStorageService, AuditService auditService) {
        this.ticketService = ticketService;
        this.userRepository = userRepository;
        this.photoStorageService = photoStorageService;
        this.auditService = auditService;
    }

    @PostMapping("/apartments/{apartmentId}/tickets")
    public ResponseEntity<?> createTicket(@PathVariable Long apartmentId,
                                          @RequestBody Map<String, String> body,
                                          Authentication auth) {
        String title = body.get("title");
        if (title == null || title.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Title is required"));
        }

        User user = userRepository.findByUsername(auth.getName()).orElseThrow();

        if (user.getRole() == Role.TENANT) {
            if (user.getApartment() == null || !user.getApartment().getId().equals(apartmentId)) {
                return ResponseEntity.status(403).body(Map.of("error", "Access denied"));
            }
        }

        Ticket ticket = ticketService.create(
                title,
                body.get("description"),
                apartmentId,
                user.getId());
        auditService.log(user.getUsername(), user.getRole().name(), "TICKET_CREATED",
                "Created ticket #" + ticket.getId() + " in apartment #" + apartmentId + ": " + title, null);
        return ResponseEntity.ok(ticket);
    }

    @GetMapping("/apartments/{apartmentId}/tickets")
    public List<Ticket> getTicketsByApartment(@PathVariable Long apartmentId, Authentication auth) {
        User user = userRepository.findByUsername(auth.getName()).orElseThrow();

        if (user.getRole() == Role.TENANT) {
            if (user.getApartment() == null || !user.getApartment().getId().equals(apartmentId)) {
                return List.of();
            }
        }

        return ticketService.findByApartmentId(apartmentId);
    }

    @GetMapping("/tickets")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN')")
    public List<Ticket> getAllTickets(Authentication auth) {
        User user = userRepository.findByUsername(auth.getName()).orElseThrow();
        List<Ticket> tickets = ticketService.findAll();
        for (Ticket ticket : tickets) {
            ticketService.markAsRead(ticket.getId(), user.getId());
        }
        return tickets;
    }

    @GetMapping("/tickets/unread/count")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN')")
    public Map<String, Long> getUnreadCount(Authentication auth) {
        User user = userRepository.findByUsername(auth.getName()).orElseThrow();
        return Map.of("count", ticketService.countUnreadForUser(user.getId()));
    }

    @GetMapping("/tickets/unread")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN')")
    public List<Ticket> getUnreadTickets(Authentication auth) {
        User user = userRepository.findByUsername(auth.getName()).orElseThrow();
        return ticketService.findUnreadForUser(user.getId());
    }

    @PostMapping("/tickets/{id}/read")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN')")
    public ResponseEntity<?> markTicketAsRead(@PathVariable Long id, Authentication auth) {
        User user = userRepository.findByUsername(auth.getName()).orElseThrow();
        ticketService.markAsRead(id, user.getId());
        return ResponseEntity.ok().build();
    }

    @GetMapping("/tickets/{id}")
    public ResponseEntity<?> getTicket(@PathVariable Long id, Authentication auth) {
        User user = userRepository.findByUsername(auth.getName()).orElseThrow();
        Ticket ticket = ticketService.findById(id);

        if (user.getRole() == Role.TENANT) {
            if (ticket.getCreatedBy() == null || !ticket.getCreatedBy().getId().equals(user.getId())) {
                return ResponseEntity.status(403).body(Map.of("error", "Access denied"));
            }
        } else {
            ticketService.markAsRead(id, user.getId());
        }

        return ResponseEntity.ok(ticket);
    }

    @PatchMapping("/tickets/{id}")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN')")
    public ResponseEntity<?> updateTicketStatus(@PathVariable Long id,
                                                @RequestBody Map<String, String> body,
                                                Authentication auth) {
        TicketStatus status = TicketStatus.valueOf(body.get("status"));
        Ticket ticket = ticketService.updateStatus(id, status);
        User user = userRepository.findByUsername(auth.getName()).orElseThrow();
        auditService.log(user.getUsername(), user.getRole().name(), "TICKET_STATUS_UPDATED",
                "Updated ticket #" + id + " status to " + status, null);
        return ResponseEntity.ok(ticket);
    }

    @PostMapping("/tickets/{id}/photos")
    public ResponseEntity<?> uploadPhoto(@PathVariable Long id,
                                         @RequestParam("file") MultipartFile file,
                                         Authentication auth) {
        User user = userRepository.findByUsername(auth.getName()).orElseThrow();
        Ticket ticket = ticketService.findById(id);

        if (user.getRole() == Role.TENANT) {
            if (ticket.getCreatedBy() == null || !ticket.getCreatedBy().getId().equals(user.getId())) {
                return ResponseEntity.status(403).body(Map.of("error", "Access denied"));
            }
            if (ticket.getPhotoPaths().size() >= 5) {
                return ResponseEntity.badRequest().body(Map.of("error", "Maximum 5 photos per ticket"));
            }
        }

        try {
            String fileName = photoStorageService.store(file);
            ticket.getPhotoPaths().add(fileName);
            ticketService.save(ticket);
            auditService.log(user.getUsername(), user.getRole().name(), "TICKET_PHOTO_UPLOADED",
                    "Uploaded photo to ticket #" + id, null);
            return ResponseEntity.ok(ticket);
        } catch (IOException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/tickets/photos/{fileName}")
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
}
