package com.apartmentmanager.repository;

import com.apartmentmanager.model.InspectionRow;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface InspectionRowRepository extends JpaRepository<InspectionRow, Long> {
    List<InspectionRow> findBySectionIdOrderBySortOrder(Long sectionId);
}
