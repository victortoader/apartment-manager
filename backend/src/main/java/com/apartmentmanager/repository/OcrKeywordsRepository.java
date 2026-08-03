package com.apartmentmanager.repository;

import com.apartmentmanager.model.OcrKeywords;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface OcrKeywordsRepository extends JpaRepository<OcrKeywords, Long> {
    Optional<OcrKeywords> findByLanguage(String language);
}
