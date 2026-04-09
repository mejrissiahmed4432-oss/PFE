package com.example.technicianmicroservice.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Document(collection = "part_requests")
public class PartRequest {
    @Id
    private String id;
    
    private String partName;
    private String category;
    private Integer quantity;
    private String priority; // Low, Medium, High
    private String description;
    private String type;
    private String specification;
    private String equipmentId;
    private String status;
    
    private String requesterId;
    private String requesterName;
    
    @CreatedDate
    private LocalDateTime createdAt;

    public PartRequest() {}

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    
    public String getPartName() { return partName; }
    public void setPartName(String partName) { this.partName = partName; }
    
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    
    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }
    
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
    
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getSpecification() { return specification; }
    public void setSpecification(String specification) { this.specification = specification; }

    public String getEquipmentId() { return equipmentId; }
    public void setEquipmentId(String equipmentId) { this.equipmentId = equipmentId; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
