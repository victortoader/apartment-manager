package com.example.demo.service;

import com.example.demo.model.Apartment;
import com.example.demo.repository.ApartmentRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ApartmentService {

    private final ApartmentRepository repository;
    private final PhotoStorageService photoStorage;

    public ApartmentService(ApartmentRepository repository, PhotoStorageService photoStorage) {
        this.repository = repository;
        this.photoStorage = photoStorage;
    }

    public List<Apartment> findAll() {
        return repository.findAll();
    }

    public Apartment findById(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Apartment not found with id: " + id));
    }

    public Apartment save(Apartment apartment) {
        return repository.save(apartment);
    }

    public void delete(Long id) {
        repository.deleteById(id);
    }
}
