package com.example.usermicroservice.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Document(collection = "tickets")
public class Ticket {
    @Id
    private String id;
    private String title;
    private String description;
    private String category; // e.g. "Software", "Hardware", "Network"
    private String priority; // e.g. "High", "Medium", "Low"
    private String status;   // e.g. "Open", "In Progress", "Resolved", "Closed"
    private String userId;   // Creator's ID
    private String createdAt;
    private String updatedAt;

    public Ticket() {}

    public void prePersist() {
        String now = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        if (this.createdAt == null) {
            this.createdAt = now;
        }
        if (this.status == null) {
            this.status = "Open";
        }
        if (this.priority == null) {
            this.priority = "Medium";
        }
        this.updatedAt = now;
    }

    public void preUpdate() {
        this.updatedAt = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public String getPriority() { return priority; }
    public void setPriority(String priority) { this.priority = priority; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
    public String getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(String updatedAt) { this.updatedAt = updatedAt; }
}
