package com.example.stockmanagermicroservice.procurement.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Document(collection = "supplier_responses")
public class SupplierResponse {

    @Id
    private String id;

    private String rfqId;           // ref → RFQ
    private String requestId;       // ref → EquipmentRequest (for convenience)
    private String supplierId;
    private String supplierName;

    // Uploaded PDF path
    private String pdfFilePath;
    private String originalFileName;

    // Quotation details (can be filled manually or extracted)
    private Double totalPrice;
    private Integer deliveryDays;
    private String notes;
    private String currency;

    private SupplierResponseStatus status;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public SupplierResponse() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getRfqId() { return rfqId; }
    public void setRfqId(String rfqId) { this.rfqId = rfqId; }

    public String getRequestId() { return requestId; }
    public void setRequestId(String requestId) { this.requestId = requestId; }

    public String getSupplierId() { return supplierId; }
    public void setSupplierId(String supplierId) { this.supplierId = supplierId; }

    public String getSupplierName() { return supplierName; }
    public void setSupplierName(String supplierName) { this.supplierName = supplierName; }

    public String getPdfFilePath() { return pdfFilePath; }
    public void setPdfFilePath(String pdfFilePath) { this.pdfFilePath = pdfFilePath; }

    public String getOriginalFileName() { return originalFileName; }
    public void setOriginalFileName(String originalFileName) { this.originalFileName = originalFileName; }

    public Double getTotalPrice() { return totalPrice; }
    public void setTotalPrice(Double totalPrice) { this.totalPrice = totalPrice; }

    public Integer getDeliveryDays() { return deliveryDays; }
    public void setDeliveryDays(Integer deliveryDays) { this.deliveryDays = deliveryDays; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }

    public SupplierResponseStatus getStatus() { return status; }
    public void setStatus(SupplierResponseStatus status) { this.status = status; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
