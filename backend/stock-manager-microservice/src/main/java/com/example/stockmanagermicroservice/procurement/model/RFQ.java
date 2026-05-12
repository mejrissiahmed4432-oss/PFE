package com.example.stockmanagermicroservice.procurement.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.List;

@Document(collection = "rfqs")
public class RFQ {

    @Id
    private String id;

    private String requestId;           // ref → EquipmentRequest
    private List<String> supplierIds;   // IDs of targeted suppliers
    private List<String> supplierEmails;

    private String pdfFilePath;         // Generated RFQ PDF path
    private List<Integer> selectedItemIndices; // Indices of the items included in this RFQ

    private LocalDateTime sentAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public RFQ() {}

    public List<Integer> getSelectedItemIndices() { return selectedItemIndices; }
    public void setSelectedItemIndices(List<Integer> selectedItemIndices) { this.selectedItemIndices = selectedItemIndices; }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getRequestId() { return requestId; }
    public void setRequestId(String requestId) { this.requestId = requestId; }

    public List<String> getSupplierIds() { return supplierIds; }
    public void setSupplierIds(List<String> supplierIds) { this.supplierIds = supplierIds; }

    public List<String> getSupplierEmails() { return supplierEmails; }
    public void setSupplierEmails(List<String> supplierEmails) { this.supplierEmails = supplierEmails; }

    public String getPdfFilePath() { return pdfFilePath; }
    public void setPdfFilePath(String pdfFilePath) { this.pdfFilePath = pdfFilePath; }

    public LocalDateTime getSentAt() { return sentAt; }
    public void setSentAt(LocalDateTime sentAt) { this.sentAt = sentAt; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
