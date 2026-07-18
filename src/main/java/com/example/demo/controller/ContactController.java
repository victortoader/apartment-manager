package com.example.demo.controller;

import com.example.demo.model.Contact;
import com.example.demo.model.User;
import com.example.demo.repository.UserRepository;
import com.example.demo.service.AuditService;
import com.example.demo.service.ContactService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class ContactController {

    private final ContactService contactService;
    private final UserRepository userRepository;
    private final AuditService auditService;

    public ContactController(ContactService contactService, UserRepository userRepository, AuditService auditService) {
        this.contactService = contactService;
        this.userRepository = userRepository;
        this.auditService = auditService;
    }

    @GetMapping("/apartments/{apartmentId}/contacts")
    public List<Contact> getContacts(@PathVariable Long apartmentId) {
        return contactService.findByApartmentId(apartmentId);
    }

    @PostMapping("/apartments/{apartmentId}/contacts")
    @PreAuthorize("hasRole('OWNER')")
    public ResponseEntity<?> createContact(@PathVariable Long apartmentId,
                                           @RequestBody Map<String, String> body,
                                           Authentication auth) {
        String name = body.get("name");
        String value = body.get("value");
        if (name == null || name.isBlank() || value == null || value.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Name and value are required"));
        }
        Contact contact = contactService.create(apartmentId, name.trim(), value.trim());
        User user = userRepository.findByUsername(auth.getName()).orElseThrow();
        auditService.log(user.getUsername(), user.getRole().name(), "CONTACT_CREATED",
                "Created contact on apartment #" + apartmentId + ": " + name.trim(), null);
        return ResponseEntity.ok(contact);
    }

    @PutMapping("/contacts/{id}")
    @PreAuthorize("hasRole('OWNER')")
    public ResponseEntity<?> updateContact(@PathVariable Long id,
                                           @RequestBody Map<String, String> body,
                                           Authentication auth) {
        String name = body.get("name");
        String value = body.get("value");
        if (name == null || name.isBlank() || value == null || value.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Name and value are required"));
        }
        Contact contact = contactService.update(id, name.trim(), value.trim());
        User user = userRepository.findByUsername(auth.getName()).orElseThrow();
        auditService.log(user.getUsername(), user.getRole().name(), "CONTACT_UPDATED",
                "Updated contact #" + id + ": " + name.trim(), null);
        return ResponseEntity.ok(contact);
    }

    @DeleteMapping("/contacts/{id}")
    @PreAuthorize("hasRole('OWNER')")
    public ResponseEntity<?> deleteContact(@PathVariable Long id, Authentication auth) {
        contactService.delete(id);
        User user = userRepository.findByUsername(auth.getName()).orElseThrow();
        auditService.log(user.getUsername(), user.getRole().name(), "CONTACT_DELETED",
                "Deleted contact #" + id, null);
        return ResponseEntity.ok().build();
    }
}
