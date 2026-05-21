package com.medina.app.model;

public class Notification {
    private String id;
    private String title;
    private String message;
    private String type; // SUCCESS, INFO, ERROR
    private String category;
    private boolean read;
    private String createdAt;

    public Notification() {}

    // Constructor matching DashboardActivity mock data
    public Notification(String id, String title, String message, String createdAt, boolean read) {
        this.id = id;
        this.title = title;
        this.message = message;
        this.createdAt = createdAt;
        this.read = read;
    }

    public Notification(String id, String title, String message, String type, String category, boolean read, String createdAt) {
        this.id = id;
        this.title = title;
        this.message = message;
        this.type = type;
        this.category = category;
        this.read = read;
        this.createdAt = createdAt;
    }

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

    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
}
