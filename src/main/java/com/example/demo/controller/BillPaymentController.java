package com.example.demo.controller;

import com.example.demo.model.Apartment;
import com.example.demo.model.BillPayment;
import com.example.demo.model.User;
import com.example.demo.repository.UserRepository;
import com.example.demo.service.ApartmentService;
import com.example.demo.service.BillPaymentService;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

@RestController
@RequestMapping("/api")
public class BillPaymentController {

    private final BillPaymentService billPaymentService;
    private final ApartmentService apartmentService;
    private final UserRepository userRepository;

    public BillPaymentController(BillPaymentService billPaymentService,
                                 ApartmentService apartmentService,
                                 UserRepository userRepository) {
        this.billPaymentService = billPaymentService;
        this.apartmentService = apartmentService;
        this.userRepository = userRepository;
    }

    @GetMapping("/apartments/{id}/bills")
    public ResponseEntity<List<BillPayment>> getBills(@PathVariable Long id, Authentication auth) {
        User user = userRepository.findByUsername(auth.getName()).orElseThrow();
        Apartment apartment = apartmentService.findById(id);

        if (user.getRole() == com.example.demo.model.Role.TENANT) {
            if (user.getApartment() == null || !user.getApartment().getId().equals(id)) {
                return ResponseEntity.status(403).build();
            }
        }

        return ResponseEntity.ok(billPaymentService.findByApartmentId(id));
    }

    @PostMapping("/apartments/{id}/bills")
    public ResponseEntity<BillPayment> uploadBill(@PathVariable Long id,
                                                   @RequestParam("file") MultipartFile file,
                                                   @RequestParam(value = "billType", defaultValue = "Other Payments") String billType,
                                                   Authentication auth) {
        User user = userRepository.findByUsername(auth.getName()).orElseThrow();

        if (user.getRole() == com.example.demo.model.Role.TENANT) {
            if (user.getApartment() == null || !user.getApartment().getId().equals(id)) {
                return ResponseEntity.status(403).build();
            }
        } else if (user.getRole() != com.example.demo.model.Role.OWNER && user.getRole() != com.example.demo.model.Role.ADMIN) {
            return ResponseEntity.status(403).build();
        }

        try {
            Apartment apartment = apartmentService.findById(id);
            BillPayment bill = billPaymentService.upload(apartment, user, file, billType);
            return ResponseEntity.ok(bill);
        } catch (IOException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/bills/{fileName}")
    public ResponseEntity<Resource> getBillFile(@PathVariable String fileName) {
        try {
            Path filePath = billPaymentService.load(fileName);
            Resource resource = new UrlResource(filePath.toUri());
            if (resource.exists()) {
                String contentType = "application/octet-stream";
                if (fileName.toLowerCase().endsWith(".pdf")) contentType = "application/pdf";
                else if (fileName.toLowerCase().endsWith(".jpg") || fileName.toLowerCase().endsWith(".jpeg")) contentType = "image/jpeg";
                else if (fileName.toLowerCase().endsWith(".png")) contentType = "image/png";

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

    @DeleteMapping("/bills/{id}")
    @PreAuthorize("hasRole('OWNER')")
    public ResponseEntity<Void> deleteBill(@PathVariable Long id) {
        billPaymentService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
