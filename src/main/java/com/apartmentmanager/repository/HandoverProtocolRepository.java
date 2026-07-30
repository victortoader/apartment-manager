package com.apartmentmanager.repository;

import com.apartmentmanager.model.HandoverProtocol;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface HandoverProtocolRepository extends JpaRepository<HandoverProtocol, Long> {
    List<HandoverProtocol> findByApartmentId(Long apartmentId);

    java.util.Optional<HandoverProtocol> findByFileName(String fileName);

    void deleteByApartment(com.apartmentmanager.model.Apartment apartment);
}
