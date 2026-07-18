package com.apartmentmanager.repository;

import com.apartmentmanager.model.Apartment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface ApartmentRepository extends JpaRepository<Apartment, Long> {

    @Query("SELECT DISTINCT a FROM Apartment a LEFT JOIN FETCH a.tenants")
    List<Apartment> findAllWithTenants();

    @Query("SELECT DISTINCT a FROM Apartment a LEFT JOIN FETCH a.tenants WHERE a.id = :id")
    Optional<Apartment> findByIdWithTenants(Long id);
}
