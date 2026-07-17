package com.example.demo.repository;

import com.example.demo.model.Apartment;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ApartmentRepository extends JpaRepository<Apartment, Long> {

    @EntityGraph(attributePaths = {"tenants"})
    List<Apartment> findAll();

    @EntityGraph(attributePaths = {"tenants"})
    Optional<Apartment> findById(Long id);
}
