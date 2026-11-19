package com.apartmentmanager.controller;

import com.apartmentmanager.audit.Audited;
import com.apartmentmanager.model.Contact;
import com.apartmentmanager.model.Role;
import com.apartmentmanager.model.User;
import com.apartmentmanager.repository.UserRepository;
import com.apartmentmanager.service.ContactService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ContactController {

    private final ContactService contactService;
    private final UserRepository userRepository;

    @GetMapping("/apartments/{apartmentId}/contacts")
    public ResponseEntity<List<Contact>> getContacts(@PathVariable Long apartmentId, Authentication auth) {
        User user = userRepository.findByUsername(auth.getName()).orElseThrow();

        if (user.getRole() == Role.TENANT) {
            if (user.getApartment() == null || !user.getApartment().getId().equals(apartmentId)) {
                return ResponseEntity.status(403).build();
            }
        }

        return ResponseEntity.ok(contactService.findByApartmentId(apartmentId));
    }

    @Audited(action = "CONTACT_CREATED", message = "Created contact on apartment #{apartmentId}: {body['name']}")
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

    @Audited(action = "CONTACT_UPDATED", message = "Updated contact #{id}: {body['name']}")
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

    @Audited(action = "CONTACT_DELETED", message = "Deleted contact #{id}")
    @DeleteMapping("/contacts/{id}")
    @PreAuthorize("hasRole('OWNER')")
    public ResponseEntity<?> deleteContact(@PathVariable Long id) {
        contactService.delete(id);
        return ResponseEntity.ok().build();
    }
}
