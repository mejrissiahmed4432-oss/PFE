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

    private String status; // e.g. "SENT", "FAILED"
    private List<SupplierDeliveryStatus> deliveryStatuses; // Tracks email status per supplier

    public static class SupplierDeliveryStatus {
        private String supplierId;
        private String supplierEmail;
        private String supplierName;
        private String status; // "SENT", "FAILED"
        private String errorReason;

        public SupplierDeliveryStatus() {}

        public SupplierDeliveryStatus(String supplierId, String supplierEmail, String supplierName, String status, String errorReason) {
            this.supplierId = supplierId;
            this.supplierEmail = supplierEmail;
            this.supplierName = supplierName;
            this.status = status;
            this.errorReason = errorReason;
        }

        public String getSupplierId() { return supplierId; }
        public void setSupplierId(String supplierId) { this.supplierId = supplierId; }

        public String getSupplierEmail() { return supplierEmail; }
        public void setSupplierEmail(String supplierEmail) { this.supplierEmail = supplierEmail; }

        public String getSupplierName() { return supplierName; }
        public void setSupplierName(String supplierName) { this.supplierName = supplierName; }

        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }

        public String getErrorReason() { return errorReason; }
        public void setErrorReason(String errorReason) { this.errorReason = errorReason; }
    }

    public RFQ() {}

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public List<SupplierDeliveryStatus> getDeliveryStatuses() { return deliveryStatuses; }
    public void setDeliveryStatuses(List<SupplierDeliveryStatus> deliveryStatuses) { this.deliveryStatuses = deliveryStatuses; }

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
