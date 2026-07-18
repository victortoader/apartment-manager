package com.example.demo.controller;

import com.example.demo.model.AuditLog;
import com.example.demo.repository.AuditLogRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/audit")
public class AuditController {

    private final AuditLogRepository auditLogRepository;

    public AuditController(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    @GetMapping
    @PreAuthorize("hasRole('OWNER')")
    public ResponseEntity<List<AuditLog>> getLogs(@RequestParam(required = false) String username) {
        if (username != null && !username.isEmpty()) {
            return ResponseEntity.ok(auditLogRepository.findByUsernameOrderByTimestampDesc(username));
        }
        return ResponseEntity.ok(auditLogRepository.findAllByOrderByTimestampDesc());
    }
}
