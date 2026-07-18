package com.example.demo.service;

import com.example.demo.model.Apartment;
import com.example.demo.model.Role;
import com.example.demo.model.User;
import com.example.demo.repository.ApartmentRepository;
import com.example.demo.repository.UserRepository;
import jakarta.annotation.PostConstruct;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final ApartmentRepository apartmentRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, ApartmentRepository apartmentRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.apartmentRepository = apartmentRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @PostConstruct
    public void init() {
        if (userRepository.count() == 0) {
            userRepository.save(new User("owner", passwordEncoder.encode("owner"), Role.OWNER, "owner@example.com"));
            userRepository.save(new User("admin", passwordEncoder.encode("admin"), Role.ADMIN, "admin@example.com"));
            userRepository.save(new User("tenant", passwordEncoder.encode("tenant"), Role.TENANT, "tenant@example.com"));
        }
    }

    public List<User> findAll() {
        return userRepository.findAll();
    }

    public User findById(Long id) {
        return userRepository.findById(id).orElseThrow(() -> new RuntimeException("User not found"));
    }

    public User findByUsername(String username) {
        return userRepository.findByUsername(username).orElseThrow(() -> new RuntimeException("User not found"));
    }

    public User createUser(String username, String password, Role role, String email) {
        if (userRepository.existsByUsername(username)) {
            throw new RuntimeException("Username already exists");
        }
        User user = new User(username, passwordEncoder.encode(password), role, email);
        return userRepository.save(user);
    }

    public User assignApartment(Long userId, Long apartmentId) {
        User user = findById(userId);
        Apartment apartment = apartmentRepository.findById(apartmentId)
                .orElseThrow(() -> new RuntimeException("Apartment not found"));
        user.setApartment(apartment);
        return userRepository.save(user);
    }

    public void deleteUser(Long id) {
        userRepository.deleteById(id);
    }
}
