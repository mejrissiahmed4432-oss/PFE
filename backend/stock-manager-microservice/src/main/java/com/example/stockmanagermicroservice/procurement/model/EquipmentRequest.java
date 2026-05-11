package com.example.stockmanagermicroservice.procurement.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.List;

@Document(collection = "equipment_requests")
public class EquipmentRequest {

    @Id
    private String id;

    private String createdByUserId;
    private String createdByName;

    private List<EquipmentRequestItem> items;
    private String notes;

    private RequestStatus status;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Rejection reason (set by IT Manager)
    private String rejectionReason;
    private String supplierName;

    public EquipmentRequest() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getCreatedByUserId() { return createdByUserId; }
    public void setCreatedByUserId(String createdByUserId) { this.createdByUserId = createdByUserId; }

    public String getCreatedByName() { return createdByName; }
    public void setCreatedByName(String createdByName) { this.createdByName = createdByName; }

    public List<EquipmentRequestItem> getItems() { return items; }
    public void setItems(List<EquipmentRequestItem> items) { this.items = items; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public RequestStatus getStatus() { return status; }
    public void setStatus(RequestStatus status) { this.status = status; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public String getRejectionReason() { return rejectionReason; }
    public void setRejectionReason(String rejectionReason) { this.rejectionReason = rejectionReason; }

    public String getSupplierName() { return supplierName; }
    public void setSupplierName(String supplierName) { this.supplierName = supplierName; }
}
