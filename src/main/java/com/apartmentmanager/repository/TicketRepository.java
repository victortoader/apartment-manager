package com.apartmentmanager.repository;

import com.apartmentmanager.model.Ticket;
import com.apartmentmanager.model.TicketStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface TicketRepository extends JpaRepository<Ticket, Long> {

    List<Ticket> findByApartmentIdOrderByCreatedAtDesc(Long apartmentId);

    long countByApartmentIdAndStatus(Long apartmentId, TicketStatus status);

    @Query("SELECT t FROM Ticket t LEFT JOIN FETCH t.apartment LEFT JOIN FETCH t.createdBy ORDER BY t.createdAt DESC")
    List<Ticket> findAllWithApartmentAndUser();

    @Query("SELECT t FROM Ticket t LEFT JOIN FETCH t.apartment LEFT JOIN FETCH t.createdBy WHERE t.apartment.id = :apartmentId ORDER BY t.createdAt DESC")
    List<Ticket> findByApartmentIdWithUser(Long apartmentId);

    @Query("SELECT t FROM Ticket t LEFT JOIN FETCH t.apartment LEFT JOIN FETCH t.createdBy WHERE t.createdBy.id = :userId ORDER BY t.createdAt DESC")
    List<Ticket> findByCreatedByIdWithUser(Long userId);

    void deleteByApartment(com.apartmentmanager.model.Apartment apartment);

    @Query("SELECT COUNT(t) FROM Ticket t WHERE t NOT IN (SELECT t2 FROM Ticket t2 JOIN t2.readBy u WHERE u.id = :userId)")
    long countUnreadForUser(Long userId);

    @Query("SELECT t FROM Ticket t LEFT JOIN FETCH t.apartment LEFT JOIN FETCH t.createdBy WHERE t NOT IN (SELECT t2 FROM Ticket t2 JOIN t2.readBy u WHERE u.id = :userId) ORDER BY t.createdAt DESC")
    List<Ticket> findUnreadForUser(Long userId);
}
