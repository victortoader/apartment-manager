package com.example.demo.repository;

import com.example.demo.model.HandoverProtocol;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface HandoverProtocolRepository extends JpaRepository<HandoverProtocol, Long> {
    List<HandoverProtocol> findByApartmentId(Long apartmentId);

    void deleteByApartment(com.example.demo.model.Apartment apartment);
}
