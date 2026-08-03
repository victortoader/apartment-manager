package com.apartmentmanager.repository;

import com.apartmentmanager.model.Inspection;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface InspectionRepository extends JpaRepository<Inspection, Long> {
    List<Inspection> findByApartmentIdOrderByCreatedAtDesc(Long apartmentId);
}
