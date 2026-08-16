package com.apartmentmanager.controller;

import com.apartmentmanager.model.*;
import com.apartmentmanager.service.InspectionService;
import com.apartmentmanager.service.PhotoStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class InspectionController {

    private final InspectionService inspectionService;
    private final PhotoStorageService photoStorage;

    @GetMapping("/apartments/{apartmentId}/inspections")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN')")
    public List<Inspection> getInspections(@PathVariable Long apartmentId) {
        return inspectionService.getByApartment(apartmentId);
    }

    @GetMapping("/inspections/{id}")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN')")
    public Inspection getInspection(@PathVariable Long id) {
        return inspectionService.getById(id);
    }

    @PostMapping("/apartments/{apartmentId}/inspections")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN')")
    public Inspection createInspection(@PathVariable Long apartmentId, @RequestBody Inspection inspection) {
        return inspectionService.create(apartmentId, inspection);
    }

    @PutMapping("/inspections/{id}")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN')")
    public Inspection updateInspection(@PathVariable Long id, @RequestBody Inspection inspection) {
        return inspectionService.update(id, inspection);
    }

    @DeleteMapping("/inspections/{id}")
    @PreAuthorize("hasRole('OWNER')")
    public ResponseEntity<Void> deleteInspection(@PathVariable Long id) {
        inspectionService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/inspections/{id}/sections")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN')")
    public InspectionSection addSection(@PathVariable Long id, @RequestBody InspectionSection section) {
        return inspectionService.addSection(id, section);
    }

    @PutMapping("/inspections/sections/{sectionId}")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN')")
    public InspectionSection updateSection(@PathVariable Long sectionId, @RequestBody InspectionSection section) {
        return inspectionService.updateSection(sectionId, section);
    }

    @DeleteMapping("/inspections/sections/{sectionId}")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN')")
    public ResponseEntity<Void> deleteSection(@PathVariable Long sectionId) {
        inspectionService.deleteSection(sectionId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/inspections/sections/{sectionId}/rows")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN')")
    public InspectionRow addRow(@PathVariable Long sectionId, @RequestBody InspectionRow row) {
        return inspectionService.addRow(sectionId, row);
    }

    @PutMapping("/inspections/rows/{rowId}")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN')")
    public InspectionRow updateRow(@PathVariable Long rowId, @RequestBody InspectionRow row) {
        return inspectionService.updateRow(rowId, row);
    }

    @DeleteMapping("/inspections/rows/{rowId}")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN')")
    public ResponseEntity<Void> deleteRow(@PathVariable Long rowId) {
        inspectionService.deleteRow(rowId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/inspections/rows/{rowId}/photos")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN')")
    public ResponseEntity<InspectionRowPhoto> uploadPhoto(@PathVariable Long rowId,
                                                           @RequestParam("file") MultipartFile file) {
        try {
            return ResponseEntity.ok(inspectionService.uploadPhoto(rowId, file));
        } catch (IOException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @DeleteMapping("/inspections/photos/{photoId}")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN')")
    public ResponseEntity<Void> deletePhoto(@PathVariable Long photoId) {
        inspectionService.deletePhoto(photoId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/inspections/{id}/generate")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN')")
    public ResponseEntity<?> generatePdf(@PathVariable Long id,
                                         @RequestBody(required = false) Map<String, String> labels) {
        try {
            HandoverProtocol protocol = inspectionService.generatePdf(id, labels);
            return ResponseEntity.ok(protocol);
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(java.util.Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(java.util.Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/inspections/{id}/photos")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN')")
    public ResponseEntity<?> downloadPhotos(@PathVariable Long id) {
        try {
            byte[] data = inspectionService.downloadPhotosZip(id);
            return ResponseEntity.ok()
                    .contentType(org.springframework.http.MediaType.APPLICATION_OCTET_STREAM)
                    .header(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"Fotos_" + id + ".zip\"")
                    .body(data);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(java.util.Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/inspections/photos/{fileName}")
    public ResponseEntity<org.springframework.core.io.Resource> getPhoto(@PathVariable String fileName) {
        try {
            java.nio.file.Path filePath = photoStorage.load(fileName);
            org.springframework.core.io.Resource resource = new org.springframework.core.io.UrlResource(filePath.toUri());
            if (resource.exists()) {
                return ResponseEntity.ok()
                        .contentType(org.springframework.http.MediaType.IMAGE_JPEG)
                        .header(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION, "inline")
                        .body(resource);
            }
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }
}
