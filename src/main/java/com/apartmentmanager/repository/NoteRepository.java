package com.apartmentmanager.repository;

import com.apartmentmanager.model.Note;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NoteRepository extends JpaRepository<Note, Long> {
    List<Note> findByApartmentIdOrderByCreatedAtDesc(Long apartmentId);
}
