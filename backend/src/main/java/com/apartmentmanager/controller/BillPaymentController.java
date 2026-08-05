package com.apartmentmanager.controller;

import com.apartmentmanager.audit.Audited;
import com.apartmentmanager.model.Apartment;
import com.apartmentmanager.model.BillPayment;
import com.apartmentmanager.model.User;
import com.apartmentmanager.repository.UserRepository;
import com.apartmentmanager.service.ApartmentService;
import com.apartmentmanager.service.BillPaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import jakarta.servlet.http.HttpServletRequest;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class BillPaymentController {

    private final BillPaymentService billPaymentService;
    private final ApartmentService apartmentService;
    private final UserRepository userRepository;

    @GetMapping("/apartments/{id}/bills")
    public ResponseEntity<List<BillPayment>> getBills(@PathVariable Long id, Authentication auth) {
        User user = userRepository.findByUsername(auth.getName()).orElseThrow();
        Apartment apartment = apartmentService.findById(id);

        if (user.getRole() == com.apartmentmanager.model.Role.TENANT) {
            if (user.getApartment() == null || !user.getApartment().getId().equals(id)) {
                return ResponseEntity.status(403).build();
            }
        }

        return ResponseEntity.ok(billPaymentService.findByApartmentId(id));
    }

    @Audited(action = "BILL_UPLOADED", message = "Uploaded bill for apartment #{id} ({billType}): {file.originalFilename}")
    @PostMapping("/apartments/{id}/bills")
    public ResponseEntity<BillPayment> uploadBill(@PathVariable Long id,
                                                    @RequestParam("file") MultipartFile file,
                                                    @RequestParam(value = "billType", defaultValue = "Other Payments") String billType,
                                                    @RequestParam(value = "documentType", defaultValue = "bill") String documentType,
                                                    Authentication auth) {
        User user = userRepository.findByUsername(auth.getName()).orElseThrow();

        if (user.getRole() == com.apartmentmanager.model.Role.TENANT) {
            if (user.getApartment() == null || !user.getApartment().getId().equals(id)) {
                return ResponseEntity.status(403).build();
            }
        } else if (user.getRole() != com.apartmentmanager.model.Role.OWNER && user.getRole() != com.apartmentmanager.model.Role.ADMIN) {
            return ResponseEntity.status(403).build();
        }

        try {
            Apartment apartment = apartmentService.findById(id);
            BillPayment bill = billPaymentService.uploadFromBytes(apartment, user, file.getBytes(), file.getOriginalFilename(), file.getContentType(), billType, documentType);
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

    @Audited(action = "BILL_DELETED", message = "Deleted bill #{id}")
    @DeleteMapping("/bills/{id}")
    @PreAuthorize("hasRole('OWNER')")
    public ResponseEntity<Void> deleteBill(@PathVariable Long id) {
        billPaymentService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @Audited(action = "BILL_ANALYZED", message = "Analyzed bill #{id} - extracted: {result.extractedAmount}")
    @PostMapping("/bills/{id}/analyze")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN')")
    public ResponseEntity<?> analyzeBill(@PathVariable Long id) {
        try {
            BillPayment bill = billPaymentService.analyze(id);
            return ResponseEntity.ok(bill);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @Audited(action = "BILL_AMOUNT_UPDATED", message = "Updated bill #{id} amount to: {body['amount']} {body['currency']}")
    @PutMapping("/bills/{id}/amount")
    public ResponseEntity<?> updateBillAmount(@PathVariable Long id,
                                              @RequestBody Map<String, Object> body) {
        Double amount = null;
        if (body.containsKey("amount")) {
            Object val = body.get("amount");
            if (val instanceof Number n) {
                amount = n.doubleValue();
            }
        }
        String currency = null;
        if (body.containsKey("currency")) {
            Object val = body.get("currency");
            if (val instanceof String s) {
                currency = s;
            }
        }

        BillPayment bill = billPaymentService.updateAmount(id, amount, currency);
        return ResponseEntity.ok(bill);
    }
}
