package com.example.demo.controller;

import com.example.demo.model.Role;
import com.example.demo.model.User;
import com.example.demo.repository.UserRepository;
import com.example.demo.service.AuditService;
import com.example.demo.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/users")
public class UserManagementController {

    private final UserService userService;
    private final UserRepository userRepository;
    private final AuditService auditService;

    public UserManagementController(UserService userService, UserRepository userRepository, AuditService auditService) {
        this.userService = userService;
        this.userRepository = userRepository;
        this.auditService = auditService;
    }

    @GetMapping
    @PreAuthorize("hasRole('OWNER')")
    public List<User> getAll() {
        return userService.findAll();
    }

    @PostMapping
    @PreAuthorize("hasRole('OWNER')")
    public ResponseEntity<?> create(@RequestBody Map<String, String> body, Authentication auth) {
        try {
            String username = body.get("username");
            String password = body.get("password");
            String email = body.get("email");
            Role role = Role.valueOf(body.get("role"));
            User user = userService.createUser(username, password, role, email);
            User actor = userRepository.findByUsername(auth.getName()).orElseThrow();
            auditService.log(actor.getUsername(), actor.getRole().name(), "USER_CREATED",
                    "Created user '" + username + "' with role " + role, null);
            return ResponseEntity.ok(user);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/{id}/apartment")
    @PreAuthorize("hasRole('OWNER')")
    public ResponseEntity<?> assignApartment(@PathVariable Long id, @RequestBody Map<String, Long> body, Authentication auth) {
        try {
            User user = userService.assignApartment(id, body.get("apartmentId"));
            User actor = userRepository.findByUsername(auth.getName()).orElseThrow();
            auditService.log(actor.getUsername(), actor.getRole().name(), "APARTMENT_ASSIGNED",
                    "Assigned apartment #" + body.get("apartmentId") + " to user #" + id + " '" + user.getUsername() + "'", null);
            return ResponseEntity.ok(user);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('OWNER')")
    public ResponseEntity<Void> delete(@PathVariable Long id, Authentication auth) {
        User target = userService.findById(id);
        String actorUsername = auth.getName();
        User actor = userRepository.findByUsername(actorUsername).orElseThrow();
        String actorRole = actor.getRole().name();
        String targetUsername = target.getUsername();
        userService.deleteUser(id);
        auditService.log(actorUsername, actorRole, "USER_DELETED",
                "Deleted user #" + id + " '" + targetUsername + "'", null);
        return ResponseEntity.noContent().build();
    }
}
