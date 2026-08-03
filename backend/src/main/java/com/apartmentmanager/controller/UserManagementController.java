package com.apartmentmanager.controller;

import com.apartmentmanager.audit.Audited;
import com.apartmentmanager.model.Role;
import com.apartmentmanager.model.User;
import com.apartmentmanager.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserManagementController {

    private final UserService userService;

    @GetMapping
    @PreAuthorize("hasRole('OWNER')")
    public List<User> getAll() {
        return userService.findAll();
    }

    @Audited(action = "USER_CREATED", message = "Created user '{body['username']}' with role {body['role']}")
    @PostMapping
    @PreAuthorize("hasRole('OWNER')")
    public ResponseEntity<?> create(@RequestBody Map<String, String> body) {
        try {
            String username = body.get("username");
            String password = body.get("password");
            String email = body.get("email");
            Role role = Role.valueOf(body.get("role"));
            User user = userService.createUser(username, password, role, email);
            return ResponseEntity.ok(user);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @Audited(action = "APARTMENT_ASSIGNED", message = "Assigned apartment #{body['apartmentId']} to user #{id} '{result.username}'")
    @PutMapping("/{id}/apartment")
    @PreAuthorize("hasRole('OWNER')")
    public ResponseEntity<?> assignApartment(@PathVariable Long id, @RequestBody Map<String, Long> body) {
        try {
            User user = userService.assignApartment(id, body.get("apartmentId"));
            return ResponseEntity.ok(user);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @Audited(action = "USER_DELETED", message = "Deleted user #{id}")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('OWNER')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        userService.deleteUser(id);
        return ResponseEntity.noContent().build();
    }
}
