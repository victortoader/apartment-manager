package com.apartmentmanager.repository;

import com.apartmentmanager.model.Contact;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ContactRepository extends JpaRepository<Contact, Long> {
    List<Contact> findByApartmentId(Long apartmentId);
}
