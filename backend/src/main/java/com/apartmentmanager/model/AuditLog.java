package com.apartmentmanager.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "audit_logs")
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String username;

    private String role;

    private String action;

    @Column(columnDefinition = "TEXT")
    private String details;

    private String ipAddress;

    private LocalDateTime timestamp = LocalDateTime.now();

    public AuditLog(String username, String role, String action, String details, String ipAddress) {
        this.username = username;
        this.role = role;
        this.action = action;
        this.details = details;
        this.ipAddress = ipAddress;
    }
}
