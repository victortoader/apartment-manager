package com.apartmentmanager.service;

import com.apartmentmanager.model.Apartment;
import com.apartmentmanager.model.BillPayment;
import com.apartmentmanager.model.User;
import com.apartmentmanager.repository.BillPaymentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.*;

@Service
public class BillPaymentService {

    private static final Logger log = LoggerFactory.getLogger(BillPaymentService.class);
    private static final long OCR_TIMEOUT_MINUTES = 3;

    private final BillPaymentRepository billPaymentRepository;
    private final OcrService ocrService;
    private final Path uploadPath;
    private final ExecutorService ocrExecutor = Executors.newCachedThreadPool(r -> {
        Thread t = new Thread(r, "ocr-worker");
        t.setDaemon(true);
        return t;
    });

    public BillPaymentService(BillPaymentRepository billPaymentRepository, OcrService ocrService) {
        this.billPaymentRepository = billPaymentRepository;
        this.ocrService = ocrService;
        String uploadDir = System.getProperty("app.upload.dir", "uploads");
        this.uploadPath = Paths.get(uploadDir).toAbsolutePath().normalize();
        try {
            Files.createDirectories(uploadPath);
        } catch (IOException e) {
            throw new RuntimeException("Could not create upload directory", e);
        }
    }

    public BillPayment upload(Apartment apartment, User user, MultipartFile file, String billType, String documentType) throws IOException {
        String originalName = file.getOriginalFilename();
        String ext = "";
        if (originalName != null && originalName.contains(".")) {
            ext = originalName.substring(originalName.lastIndexOf("."));
        }
        String storedName = UUID.randomUUID() + ext;
        Path filePath = uploadPath.resolve(storedName);
        Files.write(filePath, file.getBytes());

        BillPayment bill = new BillPayment(originalName, storedName, file.getContentType(), billType, documentType, apartment, user);
        bill = billPaymentRepository.save(bill);

        final BillPayment billRef = bill;
        final Path filePathRef = filePath;
        CompletableFuture.runAsync(() -> {
            try {
                OcrService.OcrResult result = ocrService.analyze(filePathRef, billRef.getContentType());
                billRef.setExtractedAmount(result.getAmount());
                billRef.setExtractedCurrency(result.getCurrency());
                billRef.setOcrConfidence(result.getConfidence());
                billRef.setOcrFailed(false);
                billPaymentRepository.save(billRef);
                log.info("Auto-OCR for bill #{}: amount={} {} confidence={}", billRef.getId(), result.getAmount(), result.getCurrency(), result.getConfidence());
            } catch (Exception e) {
                log.warn("Auto-OCR failed for bill #{}: {}", billRef.getId(), e.getMessage());
                billRef.setOcrFailed(true);
                billPaymentRepository.save(billRef);
            }
        }, ocrExecutor).orTimeout(OCR_TIMEOUT_MINUTES, TimeUnit.MINUTES)
          .exceptionally(ex -> {
              log.warn("Auto-OCR timed out for bill #{} after {} minutes", billRef.getId(), OCR_TIMEOUT_MINUTES);
              billRef.setOcrFailed(true);
              billPaymentRepository.save(billRef);
              return null;
          });

        return bill;
    }

    public List<BillPayment> findByApartmentId(Long apartmentId) {
        return billPaymentRepository.findByApartmentIdWithUploader(apartmentId);
    }

    public void delete(Long id) {
        BillPayment bill = billPaymentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Bill payment not found"));
        try {
            Path filePath = uploadPath.resolve(bill.getStoredFileName());
            Files.deleteIfExists(filePath);
        } catch (IOException e) {
            // ignore
        }
        billPaymentRepository.deleteById(id);
    }

    public Path load(String storedFileName) {
        return uploadPath.resolve(storedFileName);
    }

    public BillPayment analyze(Long id) throws IOException {
        BillPayment bill = billPaymentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Bill payment not found"));

        Path filePath = uploadPath.resolve(bill.getStoredFileName());
        if (!Files.exists(filePath)) {
            throw new RuntimeException("Bill file not found on disk");
        }

        OcrService.OcrResult result = ocrService.analyze(filePath, bill.getContentType());
        bill.setExtractedAmount(result.getAmount());
        bill.setExtractedCurrency(result.getCurrency());
        bill.setOcrConfidence(result.getConfidence());

        return billPaymentRepository.save(bill);
    }

    public BillPayment updateAmount(Long id, Double amount, String currency) {
        BillPayment bill = billPaymentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Bill payment not found"));
        bill.setExtractedAmount(amount);
        if (currency != null) bill.setExtractedCurrency(currency);
        return billPaymentRepository.save(bill);
    }
}
