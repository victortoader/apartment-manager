package com.apartmentmanager.repository;

import com.apartmentmanager.model.InspectionSection;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface InspectionSectionRepository extends JpaRepository<InspectionSection, Long> {
    List<InspectionSection> findByInspectionIdOrderBySortOrder(Long inspectionId);
}
