package com.example.demo.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

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

    public AuditLog() {}

    public AuditLog(String username, String role, String action, String details, String ipAddress) {
        this.username = username;
        this.role = role;
        this.action = action;
        this.details = details;
        this.ipAddress = ipAddress;
    }

    public Long getId() { return id; }
    public String getUsername() { return username; }
    public String getRole() { return role; }
    public String getAction() { return action; }
    public String getDetails() { return details; }
    public String getIpAddress() { return ipAddress; }
    public LocalDateTime getTimestamp() { return timestamp; }
}
