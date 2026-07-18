package com.example.demo.controller;

import com.example.demo.model.Contact;
import com.example.demo.service.ContactService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class ContactController {

    private final ContactService contactService;

    public ContactController(ContactService contactService) {
        this.contactService = contactService;
    }

    @GetMapping("/apartments/{apartmentId}/contacts")
    public List<Contact> getContacts(@PathVariable Long apartmentId) {
        return contactService.findByApartmentId(apartmentId);
    }

    @PostMapping("/apartments/{apartmentId}/contacts")
    @PreAuthorize("hasRole('OWNER')")
    public ResponseEntity<?> createContact(@PathVariable Long apartmentId,
                                           @RequestBody Map<String, String> body) {
        String name = body.get("name");
        String value = body.get("value");
        if (name == null || name.isBlank() || value == null || value.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Name and value are required"));
        }
        Contact contact = contactService.create(apartmentId, name.trim(), value.trim());
        return ResponseEntity.ok(contact);
    }

    @PutMapping("/contacts/{id}")
    @PreAuthorize("hasRole('OWNER')")
    public ResponseEntity<?> updateContact(@PathVariable Long id,
                                           @RequestBody Map<String, String> body) {
        String name = body.get("name");
        String value = body.get("value");
        if (name == null || name.isBlank() || value == null || value.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Name and value are required"));
        }
        Contact contact = contactService.update(id, name.trim(), value.trim());
        return ResponseEntity.ok(contact);
    }

    @DeleteMapping("/contacts/{id}")
    @PreAuthorize("hasRole('OWNER')")
    public ResponseEntity<?> deleteContact(@PathVariable Long id) {
        contactService.delete(id);
        return ResponseEntity.ok().build();
    }
}
