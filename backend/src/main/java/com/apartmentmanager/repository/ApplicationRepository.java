package com.apartmentmanager.repository;

import com.apartmentmanager.model.Application;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ApplicationRepository extends JpaRepository<Application, Long> {
    List<Application> findByApartmentIdOrderBySubmittedAtDesc(Long apartmentId);
}
