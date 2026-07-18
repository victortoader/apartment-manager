package com.apartmentmanager.service;

import com.apartmentmanager.model.Apartment;
import com.apartmentmanager.model.BillPayment;
import com.apartmentmanager.model.User;
import com.apartmentmanager.repository.BillPaymentRepository;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.UUID;

@Service
public class BillPaymentService {

    private final BillPaymentRepository billPaymentRepository;
    private final Path uploadPath;

    public BillPaymentService(BillPaymentRepository billPaymentRepository) {
        this.billPaymentRepository = billPaymentRepository;
        String uploadDir = System.getProperty("app.upload.dir", "uploads");
        this.uploadPath = Paths.get(uploadDir).toAbsolutePath().normalize();
        try {
            Files.createDirectories(uploadPath);
        } catch (IOException e) {
            throw new RuntimeException("Could not create upload directory", e);
        }
    }

    public BillPayment upload(Apartment apartment, User user, MultipartFile file, String billType) throws IOException {
        String originalName = file.getOriginalFilename();
        String ext = "";
        if (originalName != null && originalName.contains(".")) {
            ext = originalName.substring(originalName.lastIndexOf("."));
        }
        String storedName = UUID.randomUUID() + ext;
        Path filePath = uploadPath.resolve(storedName);
        Files.write(filePath, file.getBytes());

        BillPayment bill = new BillPayment(originalName, storedName, file.getContentType(), billType, apartment, user);
        return billPaymentRepository.save(bill);
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
}
