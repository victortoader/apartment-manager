package com.apartmentmanager.controller;

import com.apartmentmanager.model.OcrKeywords;
import com.apartmentmanager.repository.OcrKeywordsRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/ocr-keywords")
public class OcrKeywordsController {

    private final OcrKeywordsRepository ocrKeywordsRepository;

    public OcrKeywordsController(OcrKeywordsRepository ocrKeywordsRepository) {
        this.ocrKeywordsRepository = ocrKeywordsRepository;
    }

    @GetMapping
    public ResponseEntity<List<OcrKeywords>> getAll() {
        return ResponseEntity.ok(ocrKeywordsRepository.findAll());
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('OWNER')")
    public ResponseEntity<?> update(@PathVariable Long id, @RequestBody Map<String, String> body) {
        OcrKeywords kw = ocrKeywordsRepository.findById(id).orElseThrow();
        if (body.containsKey("amountKeywords")) kw.setAmountKeywords(body.get("amountKeywords"));
        if (body.containsKey("languageKeywords")) kw.setLanguageKeywords(body.get("languageKeywords"));
        if (body.containsKey("defaultCurrency")) kw.setDefaultCurrency(body.get("defaultCurrency"));
        return ResponseEntity.ok(ocrKeywordsRepository.save(kw));
    }

    @PostMapping
    @PreAuthorize("hasRole('OWNER')")
    public ResponseEntity<OcrKeywords> create(@RequestBody OcrKeywords keywords) {
        return ResponseEntity.ok(ocrKeywordsRepository.save(keywords));
    }
}
