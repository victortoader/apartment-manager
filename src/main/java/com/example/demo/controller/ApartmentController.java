package com.example.demo.controller;

import com.example.demo.model.Apartment;
import com.example.demo.model.HandoverProtocol;
import com.example.demo.service.ApartmentService;
import com.example.demo.service.HandoverProtocolService;
import com.example.demo.service.PhotoStorageService;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

@RestController
@RequestMapping("/api/apartments")
@CrossOrigin(origins = "*")
public class ApartmentController {

    private final ApartmentService apartmentService;
    private final PhotoStorageService photoStorageService;
    private final HandoverProtocolService protocolService;

    public ApartmentController(ApartmentService apartmentService,
                               PhotoStorageService photoStorageService,
                               HandoverProtocolService protocolService) {
        this.apartmentService = apartmentService;
        this.photoStorageService = photoStorageService;
        this.protocolService = protocolService;
    }

    @GetMapping
    public List<Apartment> getAll() {
        return apartmentService.findAll();
    }

    @GetMapping("/{id}")
    public Apartment getById(@PathVariable Long id) {
        return apartmentService.findById(id);
    }

    @PostMapping
    public Apartment create(@RequestBody Apartment apartment) {
        return apartmentService.save(apartment);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        apartmentService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/photos")
    public ResponseEntity<Apartment> uploadPhoto(@PathVariable Long id,
                                                  @RequestParam("file") MultipartFile file) {
        try {
            String fileName = photoStorageService.store(file);
            Apartment apartment = apartmentService.findById(id);
            apartment.getPhotoPaths().add(fileName);
            apartmentService.save(apartment);
            return ResponseEntity.ok(apartment);
        } catch (IOException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/photos/{fileName}")
    public ResponseEntity<Resource> getPhoto(@PathVariable String fileName) {
        try {
            Path filePath = photoStorageService.load(fileName);
            Resource resource = new UrlResource(filePath.toUri());
            if (resource.exists()) {
                return ResponseEntity.ok()
                        .contentType(MediaType.IMAGE_JPEG)
                        .header(HttpHeaders.CONTENT_DISPOSITION, "inline")
                        .body(resource);
            }
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/{id}/protocols")
    public List<HandoverProtocol> getProtocols(@PathVariable Long id) {
        return protocolService.findByApartmentId(id);
    }

    @PostMapping("/{id}/protocols")
    public ResponseEntity<HandoverProtocol> uploadProtocol(@PathVariable Long id,
                                                           @RequestParam("file") MultipartFile file) {
        try {
            Apartment apartment = apartmentService.findById(id);
            HandoverProtocol protocol = protocolService.upload(id, file, apartment);
            return ResponseEntity.ok(protocol);
        } catch (IOException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/protocols/{fileName}")
    public ResponseEntity<Resource> getProtocolFile(@PathVariable String fileName) {
        try {
            Path filePath = protocolService.loadFile(fileName);
            Resource resource = new UrlResource(filePath.toUri());
            if (resource.exists()) {
                String contentType = determineContentType(fileName);
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

    @DeleteMapping("/protocols/{id}")
    public ResponseEntity<Void> deleteProtocol(@PathVariable Long id) {
        protocolService.delete(id);
        return ResponseEntity.noContent().build();
    }

    private String determineContentType(String fileName) {
        if (fileName.endsWith(".pdf")) return "application/pdf";
        if (fileName.endsWith(".doc") || fileName.endsWith(".docx")) return "application/msword";
        if (fileName.endsWith(".jpg") || fileName.endsWith(".jpeg")) return "image/jpeg";
        if (fileName.endsWith(".png")) return "image/png";
        return "application/octet-stream";
    }
}
