package com.example.employeemicroservice.blockchain;

import java.time.LocalDateTime;

/**
 * Représente un log d'audit lu depuis la Blockchain (Ganache).
 * DTO pur — non lié à MongoDB.
 */
public class AuditLogEntry {

    private String userId;       // CIN de l'utilisateur
    private String userName;     // Nom complet
    private String userRole;     // Rôle (ADMIN, STOCK_MANAGER, etc.)
    private String action;       // Action effectuée
    private String details;      // Détails de l'action
    private LocalDateTime timestamp;

    public AuditLogEntry() {}

    public AuditLogEntry(String userId, String userName, String userRole,
                         String action, String details, LocalDateTime timestamp) {
        this.userId = userId;
        this.userName = userName;
        this.userRole = userRole;
        this.action = action;
        this.details = details;
        this.timestamp = timestamp;
    }

    // Getters & Setters
    public String getUserId()                          { return userId; }
    public void setUserId(String userId)               { this.userId = userId; }
    public String getUserName()                        { return userName; }
    public void setUserName(String userName)           { this.userName = userName; }
    public String getUserRole()                        { return userRole; }
    public void setUserRole(String userRole)           { this.userRole = userRole; }
    public String getAction()                          { return action; }
    public void setAction(String action)               { this.action = action; }
    public String getDetails()                         { return details; }
    public void setDetails(String details)             { this.details = details; }
    public LocalDateTime getTimestamp()                { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp)  { this.timestamp = timestamp; }
}
