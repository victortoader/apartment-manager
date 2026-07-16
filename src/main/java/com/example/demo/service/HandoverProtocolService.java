package com.example.demo.service;

import com.example.demo.model.Apartment;
import com.example.demo.model.HandoverProtocol;
import com.example.demo.repository.HandoverProtocolRepository;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

@Service
public class HandoverProtocolService {

    private final HandoverProtocolRepository repository;
    private final PhotoStorageService photoStorage;

    public HandoverProtocolService(HandoverProtocolRepository repository, PhotoStorageService photoStorage) {
        this.repository = repository;
        this.photoStorage = photoStorage;
    }

    public List<HandoverProtocol> findByApartmentId(Long apartmentId) {
        return repository.findByApartmentId(apartmentId);
    }

    public HandoverProtocol upload(Long apartmentId, MultipartFile file, Apartment apartment) throws IOException {
        String storedName = photoStorage.store(file);
        HandoverProtocol protocol = new HandoverProtocol(storedName, file.getOriginalFilename(), file.getContentType(), apartment);
        return repository.save(protocol);
    }

    public Path loadFile(String fileName) {
        return photoStorage.load(fileName);
    }

    public void delete(Long id) {
        repository.deleteById(id);
    }
}
