package com.example.demo.service;

import com.example.demo.model.Apartment;
import com.example.demo.model.Contact;
import com.example.demo.repository.ApartmentRepository;
import com.example.demo.repository.ContactRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ContactService {

    private final ContactRepository contactRepository;
    private final ApartmentRepository apartmentRepository;

    public ContactService(ContactRepository contactRepository, ApartmentRepository apartmentRepository) {
        this.contactRepository = contactRepository;
        this.apartmentRepository = apartmentRepository;
    }

    public List<Contact> findByApartmentId(Long apartmentId) {
        return contactRepository.findByApartmentId(apartmentId);
    }

    public Contact create(Long apartmentId, String name, String value) {
        Apartment apartment = apartmentRepository.findById(apartmentId)
                .orElseThrow(() -> new RuntimeException("Apartment not found"));
        Contact contact = new Contact(name, value, apartment);
        return contactRepository.save(contact);
    }

    public Contact update(Long id, String name, String value) {
        Contact contact = contactRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Contact not found"));
        contact.setName(name);
        contact.setValue(value);
        return contactRepository.save(contact);
    }

    public void delete(Long id) {
        contactRepository.deleteById(id);
    }
}
