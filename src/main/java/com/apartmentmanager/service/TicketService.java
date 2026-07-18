package com.apartmentmanager.service;

import com.apartmentmanager.model.Apartment;
import com.apartmentmanager.model.Ticket;
import com.apartmentmanager.model.TicketStatus;
import com.apartmentmanager.model.User;
import com.apartmentmanager.repository.ApartmentRepository;
import com.apartmentmanager.repository.TicketRepository;
import com.apartmentmanager.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class TicketService {

    private final TicketRepository ticketRepository;
    private final ApartmentRepository apartmentRepository;
    private final UserRepository userRepository;

    public TicketService(TicketRepository ticketRepository,
                         ApartmentRepository apartmentRepository,
                         UserRepository userRepository) {
        this.ticketRepository = ticketRepository;
        this.apartmentRepository = apartmentRepository;
        this.userRepository = userRepository;
    }

    public Ticket create(String title, String description, Long apartmentId, Long userId) {
        Apartment apartment = apartmentRepository.findById(apartmentId)
                .orElseThrow(() -> new RuntimeException("Apartment not found"));
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Ticket ticket = new Ticket(title, description, apartment, user);
        return ticketRepository.save(ticket);
    }

    public List<Ticket> findAll() {
        return ticketRepository.findAllWithApartmentAndUser();
    }

    public List<Ticket> findByApartmentId(Long apartmentId) {
        return ticketRepository.findByApartmentIdWithUser(apartmentId);
    }

    public List<Ticket> findByUserId(Long userId) {
        return ticketRepository.findByCreatedByIdWithUser(userId);
    }

    public Ticket findById(Long id) {
        return ticketRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Ticket not found"));
    }

    public Ticket updateStatus(Long id, TicketStatus status) {
        Ticket ticket = ticketRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Ticket not found"));
        ticket.setStatus(status);
        ticket.setUpdatedAt(LocalDateTime.now());
        return ticketRepository.save(ticket);
    }

    public Ticket save(Ticket ticket) {
        ticket.setUpdatedAt(LocalDateTime.now());
        return ticketRepository.save(ticket);
    }

    public long countUnreadForUser(Long userId) {
        return ticketRepository.countUnreadForUser(userId);
    }

    public List<Ticket> findUnreadForUser(Long userId) {
        return ticketRepository.findUnreadForUser(userId);
    }

    public void markAsRead(Long ticketId, Long userId) {
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new RuntimeException("Ticket not found"));
        com.apartmentmanager.model.User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        ticket.getReadBy().add(user);
        ticketRepository.save(ticket);
    }
}
