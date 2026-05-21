package com.medina.app.model;

public class Alert {
    private String id;
    private String title;
    private String message;
    private String type; // WARNING, INFO, ERROR, SUCCESS
    private String category;
    private boolean read;
    private String createdAt;
    
    // Additional fields expected by AlertsFragment & AlertsAdapter
    private String severity; // CRITICAL, WARNING, INFO
    private String status;   // ACTIVE, RESOLVED
    private String resolvedBy;
    private String resolvedAt;

    public Alert() {}

    // Constructor matching the local mocks in AlertsFragment
    public Alert(String id, String title, String message, String severity, String status, String createdAt, String resolvedBy, String resolvedAt) {
        this.id = id;
        this.title = title;
        this.message = message;
        this.severity = severity;
        this.status = status;
        this.createdAt = createdAt;
        this.resolvedBy = resolvedBy;
        this.resolvedAt = resolvedAt;
        this.type = severity;
    }

    // Older constructor matching other parts of codebase
    public Alert(String id, String title, String message, String type, String category, boolean read, String createdAt) {
        this.id = id;
        this.title = title;
        this.message = message;
        this.type = type;
        this.category = category;
        this.read = read;
        this.createdAt = createdAt;
        this.severity = type;
        this.status = read ? "RESOLVED" : "ACTIVE";
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public String getType() { return type; }
    public void setType(String type) { 
        this.type = type; 
        this.severity = type;
    }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public boolean isRead() { return read; }
    public void setRead(boolean read) { this.read = read; }

    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }

    public String getSeverity() { return severity != null ? severity : type; }
    public void setSeverity(String severity) { 
        this.severity = severity; 
        this.type = severity;
    }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getResolvedBy() { return resolvedBy; }
    public void setResolvedBy(String resolvedBy) { this.resolvedBy = resolvedBy; }

    public String getResolvedAt() { return resolvedAt; }
    public void setResolvedAt(String resolvedAt) { this.resolvedAt = resolvedAt; }
}
