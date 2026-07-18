package com.example.demo.repository;

import com.example.demo.model.BillPayment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.List;

public interface BillPaymentRepository extends JpaRepository<BillPayment, Long> {

    List<BillPayment> findByApartmentIdOrderByUploadDateDesc(Long apartmentId);

    void deleteByApartment(com.example.demo.model.Apartment apartment);

    @Query("SELECT b FROM BillPayment b LEFT JOIN FETCH b.uploadedBy WHERE b.apartment.id = :apartmentId ORDER BY b.uploadDate DESC")
    List<BillPayment> findByApartmentIdWithUploader(Long apartmentId);
}
