package com.example.stockmanagermicroservice.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Document(collection = "alerts")
public class Alert {
    @Id
    private String id;
    
    private String title;
    private String message;
    private String type; // e.g., WARNING, INFO, ERROR, SUCCESS
    private String category; // e.g., WARRANTY, STOCK, MAINTENANCE
    private boolean read;
    private String relatedId; // ID of the equipment or supplier related to this alert
    
    @CreatedDate
    private LocalDateTime createdAt;

    public Alert() {}

    public Alert(String title, String message, String type, String category, String relatedId) {
        this.title = title;
        this.message = message;
        this.type = type;
        this.category = category;
        this.relatedId = relatedId;
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

    public boolean isRead() { return read; }
    public void setRead(boolean read) { this.read = read; }

    public String getRelatedId() { return relatedId; }
    public void setRelatedId(String relatedId) { this.relatedId = relatedId; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
