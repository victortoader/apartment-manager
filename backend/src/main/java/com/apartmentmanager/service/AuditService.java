package com.apartmentmanager.service;

import com.apartmentmanager.model.AuditLog;
import com.apartmentmanager.repository.AuditLogRepository;
import org.springframework.stereotype.Service;

@Service
public class AuditService {

    private final AuditLogRepository auditLogRepository;

    public AuditService(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    public void log(String username, String role, String action, String details, String ipAddress) {
        auditLogRepository.save(new AuditLog(username, role, action, details, ipAddress));
    }
}
