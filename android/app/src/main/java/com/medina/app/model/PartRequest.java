package com.medina.app.model;

import java.util.List;

public class PartRequest {
    private String id;
    private List<PartRequestItem> items;
    private String priority;
    private String description;
    private String status;
    private String requesterId;
    private String requesterName;
    private String createdAt;

    public PartRequest() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public List<PartRequestItem> getItems() { return items; }
    public void setItems(List<PartRequestItem> items) { this.items = items; }

    public String getPriority() { return priority; }
    public void setPriority(String priority) { this.priority = priority; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getRequesterId() { return requesterId; }
    public void setRequesterId(String requesterId) { this.requesterId = requesterId; }

    public String getRequesterName() { return requesterName; }
    public void setRequesterName(String requesterName) { this.requesterName = requesterName; }

    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
}
