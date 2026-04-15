package com.example.usermicroservice.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Document(collection = "notifications")
public class Notification {
    @Id
    private String id;
    
    private String title;
    private String message;
    private String type; // SUCCESS, INFO, ERROR, WARNING
    private String category; // PART_REQUEST, TICKET, TASK, EQUIPMENT, etc.
    private String recipientId; // Optional: If assigned directly to a user
    private boolean read;
    private String relatedId; 
    
    @CreatedDate
    private LocalDateTime createdAt;

    public Notification() {}

    public Notification(String title, String message, String type, String category, String relatedId, String recipientId) {
        this.title = title;
        this.message = message;
        this.type = type;
        this.category = category;
        this.relatedId = relatedId;
        this.recipientId = recipientId;
        this.read = false;
        this.createdAt = LocalDateTime.now();
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getRecipientId() { return recipientId; }
    public void setRecipientId(String recipientId) { this.recipientId = recipientId; }

    public boolean isRead() { return read; }
    public void setRead(boolean read) { this.read = read; }

    public String getRelatedId() { return relatedId; }
    public void setRelatedId(String relatedId) { this.relatedId = relatedId; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
