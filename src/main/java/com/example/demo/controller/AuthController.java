package com.example.demo.controller;

import com.example.demo.model.User;
import com.example.demo.repository.UserRepository;
import com.example.demo.security.JwtUtil;
import com.example.demo.service.AuditService;
import com.example.demo.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import jakarta.servlet.http.HttpServletRequest;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditService auditService;

    public AuthController(AuthenticationManager authenticationManager, JwtUtil jwtUtil, UserRepository userRepository, PasswordEncoder passwordEncoder, AuditService auditService) {
        this.authenticationManager = authenticationManager;
        this.jwtUtil = jwtUtil;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.auditService = auditService;
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> body, HttpServletRequest request) {
        String username = body.get("username");
        String password = body.get("password");
        String ip = request.getRemoteAddr();

        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(username, password)
            );
        } catch (Exception e) {
            auditService.log(username, "UNKNOWN", "LOGIN_FAILED", "Invalid credentials", ip);
            return ResponseEntity.status(401).body(Map.of("error", "Invalid credentials"));
        }

        User user = userRepository.findByUsername(username).orElseThrow();
        String token = jwtUtil.generateToken(username, user.getRole());
        auditService.log(username, user.getRole().name(), "LOGIN", "Successful login", ip);

        var response = new java.util.HashMap<String, Object>();
        response.put("token", token);
        response.put("id", user.getId());
        response.put("username", username);
        response.put("role", user.getRole().name());
        if (user.getApartment() != null) {
            response.put("apartmentId", user.getApartment().getId());
        }
        return ResponseEntity.ok(response);
    }

    @GetMapping("/me")
    public ResponseEntity<?> me() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return ResponseEntity.status(401).build();
        }

        String username = auth.getName();
        User user = userRepository.findByUsername(username).orElseThrow();

        var response = new java.util.HashMap<String, Object>();
        response.put("id", user.getId());
        response.put("username", user.getUsername());
        response.put("role", user.getRole().name());
        if (user.getApartment() != null) {
            response.put("apartmentId", user.getApartment().getId());
        }
        return ResponseEntity.ok(response);
    }

    @PostMapping("/verify-password")
    public ResponseEntity<?> verifyPassword(@RequestBody Map<String, String> body, Authentication auth) {
        String password = body.get("password");
        if (auth == null || !auth.isAuthenticated()) {
            return ResponseEntity.status(401).build();
        }
        User user = userRepository.findByUsername(auth.getName()).orElseThrow();
        if (passwordEncoder.matches(password, user.getPassword())) {
            return ResponseEntity.ok(Map.of("verified", true));
        }
        return ResponseEntity.status(401).body(Map.of("error", "Invalid password"));
    }
}
