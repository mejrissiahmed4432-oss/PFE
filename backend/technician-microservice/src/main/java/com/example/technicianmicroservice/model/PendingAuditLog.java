package com.example.technicianmicroservice.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.LocalDateTime;

@Document(collection = "pending_audit_logs")
public class PendingAuditLog {
    @Id private String id;
    private String userId, userName, userRole, action, details, ipAddress;
    private LocalDateTime createdAt;

    public PendingAuditLog() { this.createdAt = LocalDateTime.now(); }
    public PendingAuditLog(String userId, String userName, String userRole, String action, String details, String ipAddress) {
        this.userId = userId; this.userName = userName; this.userRole = userRole;
        this.action = action; this.details = details; this.ipAddress = ipAddress;
        this.createdAt = LocalDateTime.now();
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getUserId() { return userId; }
    public void setUserId(String v) { this.userId = v; }
    public String getUserName() { return userName; }
    public void setUserName(String v) { this.userName = v; }
    public String getUserRole() { return userRole; }
    public void setUserRole(String v) { this.userRole = v; }
    public String getAction() { return action; }
    public void setAction(String v) { this.action = v; }
    public String getDetails() { return details; }
    public void setDetails(String v) { this.details = v; }
    public String getIpAddress() { return ipAddress; }
    public void setIpAddress(String v) { this.ipAddress = v; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime v) { this.createdAt = v; }
}
