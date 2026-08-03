package com.apartmentmanager.service;

import com.apartmentmanager.model.Apartment;
import com.apartmentmanager.model.Note;
import com.apartmentmanager.repository.ApartmentRepository;
import com.apartmentmanager.repository.NoteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class NoteService {

    private final NoteRepository noteRepository;
    private final ApartmentRepository apartmentRepository;

    public List<Note> findByApartmentId(Long apartmentId) {
        return noteRepository.findByApartmentIdOrderByCreatedAtDesc(apartmentId);
    }

    public Note create(Long apartmentId, String content) {
        Apartment apartment = apartmentRepository.findById(apartmentId)
                .orElseThrow(() -> new RuntimeException("Apartment not found"));
        return noteRepository.save(new Note(content, apartment));
    }

    public Note update(Long id, String content) {
        Note note = noteRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Note not found"));
        note.setContent(content);
        return noteRepository.save(note);
    }

    public void delete(Long id) {
        noteRepository.deleteById(id);
    }
}
