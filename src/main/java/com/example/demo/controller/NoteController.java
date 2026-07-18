package com.example.demo.controller;

import com.example.demo.model.Note;
import com.example.demo.service.NoteService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class NoteController {

    private final NoteService noteService;

    public NoteController(NoteService noteService) {
        this.noteService = noteService;
    }

    @GetMapping("/apartments/{apartmentId}/notes")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN')")
    public List<Note> getNotes(@PathVariable Long apartmentId) {
        return noteService.findByApartmentId(apartmentId);
    }

    @PostMapping("/apartments/{apartmentId}/notes")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN')")
    public ResponseEntity<?> createNote(@PathVariable Long apartmentId,
                                        @RequestBody Map<String, String> body) {
        String content = body.get("content");
        if (content == null || content.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Content is required"));
        }
        Note note = noteService.create(apartmentId, content.trim());
        return ResponseEntity.ok(note);
    }

    @PutMapping("/notes/{id}")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN')")
    public ResponseEntity<?> updateNote(@PathVariable Long id,
                                        @RequestBody Map<String, String> body) {
        String content = body.get("content");
        if (content == null || content.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Content is required"));
        }
        Note note = noteService.update(id, content.trim());
        return ResponseEntity.ok(note);
    }

    @DeleteMapping("/notes/{id}")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN')")
    public ResponseEntity<?> deleteNote(@PathVariable Long id) {
        noteService.delete(id);
        return ResponseEntity.ok().build();
    }
}
