package com.apartmentmanager.controller;

import com.apartmentmanager.model.Note;
import com.apartmentmanager.model.User;
import com.apartmentmanager.repository.UserRepository;
import com.apartmentmanager.service.AuditService;
import com.apartmentmanager.service.NoteService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class NoteController {

    private final NoteService noteService;
    private final UserRepository userRepository;
    private final AuditService auditService;

    public NoteController(NoteService noteService, UserRepository userRepository, AuditService auditService) {
        this.noteService = noteService;
        this.userRepository = userRepository;
        this.auditService = auditService;
    }

    @GetMapping("/apartments/{apartmentId}/notes")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN')")
    public List<Note> getNotes(@PathVariable Long apartmentId) {
        return noteService.findByApartmentId(apartmentId);
    }

    @PostMapping("/apartments/{apartmentId}/notes")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN')")
    public ResponseEntity<?> createNote(@PathVariable Long apartmentId,
                                        @RequestBody Map<String, String> body,
                                        Authentication auth) {
        String content = body.get("content");
        if (content == null || content.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Content is required"));
        }
        Note note = noteService.create(apartmentId, content.trim());
        User user = userRepository.findByUsername(auth.getName()).orElseThrow();
        auditService.log(user.getUsername(), user.getRole().name(), "NOTE_CREATED",
                "Created note on apartment #" + apartmentId, null);
        return ResponseEntity.ok(note);
    }

    @PutMapping("/notes/{id}")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN')")
    public ResponseEntity<?> updateNote(@PathVariable Long id,
                                        @RequestBody Map<String, String> body,
                                        Authentication auth) {
        String content = body.get("content");
        if (content == null || content.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Content is required"));
        }
        Note note = noteService.update(id, content.trim());
        User user = userRepository.findByUsername(auth.getName()).orElseThrow();
        auditService.log(user.getUsername(), user.getRole().name(), "NOTE_UPDATED",
                "Updated note #" + id, null);
        return ResponseEntity.ok(note);
    }

    @DeleteMapping("/notes/{id}")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN')")
    public ResponseEntity<?> deleteNote(@PathVariable Long id, Authentication auth) {
        noteService.delete(id);
        User user = userRepository.findByUsername(auth.getName()).orElseThrow();
        auditService.log(user.getUsername(), user.getRole().name(), "NOTE_DELETED",
                "Deleted note #" + id, null);
        return ResponseEntity.ok().build();
    }
}
