package com.example.demo.service;

import com.example.demo.model.Apartment;
import com.example.demo.model.User;
import com.example.demo.repository.ApartmentRepository;
import com.example.demo.repository.TicketRepository;
import com.example.demo.repository.BillPaymentRepository;
import com.example.demo.repository.HandoverProtocolRepository;
import com.example.demo.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ApartmentService {

    private final ApartmentRepository repository;
    private final PhotoStorageService photoStorage;
    private final TicketRepository ticketRepository;
    private final BillPaymentRepository billPaymentRepository;
    private final HandoverProtocolRepository handoverProtocolRepository;
    private final UserRepository userRepository;

    public ApartmentService(ApartmentRepository repository, PhotoStorageService photoStorage,
                            TicketRepository ticketRepository, BillPaymentRepository billPaymentRepository,
                            HandoverProtocolRepository handoverProtocolRepository, UserRepository userRepository) {
        this.repository = repository;
        this.photoStorage = photoStorage;
        this.ticketRepository = ticketRepository;
        this.billPaymentRepository = billPaymentRepository;
        this.handoverProtocolRepository = handoverProtocolRepository;
        this.userRepository = userRepository;
    }

    public List<Apartment> findAll() {
        return repository.findAllWithTenants();
    }

    public Apartment findById(Long id) {
        return repository.findByIdWithTenants(id)
                .orElseThrow(() -> new RuntimeException("Apartment not found with id: " + id));
    }

    public Apartment save(Apartment apartment) {
        return repository.save(apartment);
    }

    @Transactional
    public void delete(Long id) {
        Apartment apartment = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Apartment not found with id: " + id));

        ticketRepository.deleteByApartment(apartment);

        billPaymentRepository.deleteByApartment(apartment);

        handoverProtocolRepository.deleteByApartment(apartment);

        for (User tenant : apartment.getTenants()) {
            tenant.setApartment(null);
            userRepository.save(tenant);
        }

        apartment.getContacts().clear();
        apartment.getPhotoPaths().clear();

        repository.deleteById(id);
    }
}
