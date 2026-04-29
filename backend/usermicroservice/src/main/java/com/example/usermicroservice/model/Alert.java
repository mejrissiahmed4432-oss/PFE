package com.example.usermicroservice.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Document(collection = "alerts")
public class Alert {
    @Id
    private String id;
    
    @Indexed
    private String key; // Unique deterministic key for deduplication (e.g., LOW_STOCK_item_123)
    
    private String title;
    private String message;
    
    private String type;
    private String priority;
    
    @Indexed
    private String status; // ACTIVE or RESOLVED
    
    private String targetType; // USER or ROLE
    private String targetId; // userId or roleName (e.g., "STOCK_MANAGER")
    
    // Optional metadata
    private String category;
    private String relatedId;
    
    @CreatedDate
    private LocalDateTime createdAt;
    private LocalDateTime lastSentAt;
    private LocalDateTime resolvedAt;

    public Alert() {}

    public Alert(String key, String title, String message, String type, String priority, String targetType, String targetId) {
        this.key = key;
        this.title = title;
        this.message = message;
        this.type = type;
        this.priority = priority;
        this.status = "ACTIVE";
        this.targetType = targetType;
        this.targetId = targetId;
        this.createdAt = LocalDateTime.now();
        this.lastSentAt = LocalDateTime.now();
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getKey() { return key; }
    public void setKey(String key) { this.key = key; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public String getPriority() { return priority; }
    public void setPriority(String priority) { this.priority = priority; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getTargetType() { return targetType; }
    public void setTargetType(String targetType) { this.targetType = targetType; }

    public String getTargetId() { return targetId; }
    public void setTargetId(String targetId) { this.targetId = targetId; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getRelatedId() { return relatedId; }
    public void setRelatedId(String relatedId) { this.relatedId = relatedId; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getLastSentAt() { return lastSentAt; }
    public void setLastSentAt(LocalDateTime lastSentAt) { this.lastSentAt = lastSentAt; }

    public LocalDateTime getResolvedAt() { return resolvedAt; }
    public void setResolvedAt(LocalDateTime resolvedAt) { this.resolvedAt = resolvedAt; }
}
