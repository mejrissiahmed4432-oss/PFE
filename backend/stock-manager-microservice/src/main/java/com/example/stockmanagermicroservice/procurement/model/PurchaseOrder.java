package com.example.stockmanagermicroservice.procurement.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Document(collection = "purchase_orders")
public class PurchaseOrder {

    @Id
    private String id;

    private String requestId;
    private String rfqId;
    private String selectedResponseId;
    private String supplierId;
    private String supplierName;

    private Double totalPrice;
    private String currency;
    private Integer deliveryDays;
    private java.util.List<EquipmentRequestItem> items;

    private RequestStatus status;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    private LocalDateTime receivedAt;
    private String receiptNotes;
    private Integer supplierRating; // 1 to 5

    public PurchaseOrder() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getRequestId() { return requestId; }
    public void setRequestId(String requestId) { this.requestId = requestId; }

    public String getRfqId() { return rfqId; }
    public void setRfqId(String rfqId) { this.rfqId = rfqId; }

    public String getSelectedResponseId() { return selectedResponseId; }
    public void setSelectedResponseId(String selectedResponseId) { this.selectedResponseId = selectedResponseId; }

    public String getSupplierId() { return supplierId; }
    public void setSupplierId(String supplierId) { this.supplierId = supplierId; }

    public String getSupplierName() { return supplierName; }
    public void setSupplierName(String supplierName) { this.supplierName = supplierName; }

    public Double getTotalPrice() { return totalPrice; }
    public void setTotalPrice(Double totalPrice) { this.totalPrice = totalPrice; }

    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }

    public Integer getDeliveryDays() { return deliveryDays; }
    public void setDeliveryDays(Integer deliveryDays) { this.deliveryDays = deliveryDays; }

    public RequestStatus getStatus() { return status; }
    public void setStatus(RequestStatus status) { this.status = status; }

    public java.util.List<EquipmentRequestItem> getItems() { return items; }
    public void setItems(java.util.List<EquipmentRequestItem> items) { this.items = items; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public LocalDateTime getReceivedAt() { return receivedAt; }
    public void setReceivedAt(LocalDateTime receivedAt) { this.receivedAt = receivedAt; }

    public String getReceiptNotes() { return receiptNotes; }
    public void setReceiptNotes(String receiptNotes) { this.receiptNotes = receiptNotes; }

    public Integer getSupplierRating() { return supplierRating; }
    public void setSupplierRating(Integer supplierRating) { this.supplierRating = supplierRating; }
}
