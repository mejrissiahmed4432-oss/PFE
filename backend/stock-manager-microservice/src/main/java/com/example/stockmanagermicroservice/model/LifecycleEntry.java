package com.example.stockmanagermicroservice.model;

import java.time.LocalDateTime;

public class LifecycleEntry {
    private String status;
    private LocalDateTime timestamp;
    private String description;
    private String actor;

    public LifecycleEntry() {}

    public LifecycleEntry(String status, LocalDateTime timestamp, String description, String actor) {
        this.status = status;
        this.timestamp = timestamp;
        this.description = description;
        this.actor = actor;
    }

    // Getters and Setters
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getActor() { return actor; }
    public void setActor(String actor) { this.actor = actor; }
}
