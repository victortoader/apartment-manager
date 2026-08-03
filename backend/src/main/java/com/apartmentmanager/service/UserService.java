package com.apartmentmanager.service;

import com.apartmentmanager.model.Apartment;
import com.apartmentmanager.model.Role;
import com.apartmentmanager.model.User;
import com.apartmentmanager.repository.ApartmentRepository;
import com.apartmentmanager.repository.UserRepository;
import jakarta.annotation.PostConstruct;
import org.springframework.security.crypto.password.PasswordEncoder;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final ApartmentRepository apartmentRepository;
    private final PasswordEncoder passwordEncoder;

    @PostConstruct
    public void init() {
        if (userRepository.count() == 0) {
            String defaultPassword = requireDefaultPassword();
            userRepository.save(new User("owner", passwordEncoder.encode(defaultPassword), Role.OWNER, "owner@example.com"));
            userRepository.save(new User("admin", passwordEncoder.encode(defaultPassword), Role.ADMIN, "admin@example.com"));
            userRepository.save(new User("tenant", passwordEncoder.encode(defaultPassword), Role.TENANT, "tenant@example.com"));
        }
    }

    private static String requireDefaultPassword() {
        return com.apartmentmanager.util.DefaultPassword.require();
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
