package com.apartmentmanager.controller;

import com.apartmentmanager.model.Apartment;
import com.apartmentmanager.model.Application;
import com.apartmentmanager.repository.ApplicationRepository;
import com.apartmentmanager.service.ApartmentService;
import com.apartmentmanager.service.PhotoStorageService;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/applications")
public class ApplicationController {

    private final ApplicationRepository applicationRepository;
    private final ApartmentService apartmentService;
    private final PhotoStorageService photoStorageService;

    public ApplicationController(ApplicationRepository applicationRepository,
                                  ApartmentService apartmentService,
                                  PhotoStorageService photoStorageService) {
        this.applicationRepository = applicationRepository;
        this.apartmentService = apartmentService;
        this.photoStorageService = photoStorageService;
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> submit(
            @RequestParam("apartmentId") Long apartmentId,
            @RequestParam("applicantName") String applicantName,
            @RequestParam("applicantEmail") String applicantEmail,
            @RequestParam(value = "applicantPhone", required = false) String applicantPhone,
            @RequestParam(value = "formData", required = false) String formData,
            @RequestParam("files") MultipartFile[] files) {
        try {
            Apartment apartment = apartmentService.findById(apartmentId);
            String[] storedNames = new String[files.length];
            String[] originalNames = new String[files.length];
            for (int i = 0; i < files.length; i++) {
                storedNames[i] = photoStorageService.store(files[i]);
                originalNames[i] = files[i].getOriginalFilename();
            }
            Application application = new Application(apartment, applicantName, applicantEmail,
                    applicantPhone, String.join(", ", storedNames), String.join(", ", originalNames), formData);
            applicationRepository.save(application);
            return ResponseEntity.ok(Map.of("success", true, "id", application.getId()));
        } catch (IOException e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "error", "File upload failed"));
        }
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN')")
    public ResponseEntity<List<Application>> getByApartment(@RequestParam("apartmentId") Long apartmentId) {
        return ResponseEntity.ok(applicationRepository.findByApartmentIdOrderBySubmittedAtDesc(apartmentId));
    }

    @GetMapping("/counts")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN')")
    public ResponseEntity<Map<Long, Integer>> getCounts() {
        List<Application> all = applicationRepository.findAll();
        Map<Long, Integer> counts = new java.util.HashMap<>();
        for (Application app : all) {
            Long aptId = app.getApartment().getId();
            counts.put(aptId, counts.getOrDefault(aptId, 0) + 1);
        }
        return ResponseEntity.ok(counts);
    }

    @GetMapping("/files/{fileName}")
    public ResponseEntity<Resource> getFile(@PathVariable String fileName) {
        try {
            Path filePath = photoStorageService.load(fileName);
            Resource resource = new UrlResource(filePath.toUri());
            if (resource.exists()) {
                String contentType = "application/octet-stream";
                if (fileName.endsWith(".pdf")) contentType = "application/pdf";
                else if (fileName.endsWith(".jpg") || fileName.endsWith(".jpeg")) contentType = "image/jpeg";
                else if (fileName.endsWith(".png")) contentType = "image/png";
                else if (fileName.endsWith(".doc") || fileName.endsWith(".docx")) contentType = "application/msword";
                return ResponseEntity.ok()
                        .contentType(MediaType.parseMediaType(contentType))
                        .header(HttpHeaders.CONTENT_DISPOSITION, "inline")
                        .body(resource);
            }
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }
}
