package com.example.stockmanagermicroservice.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDate;
import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.ArrayList;
import java.util.List;

@Document(collection = "equipment")
public class Equipment {
    @Id
    private String id;
    
    private String equipmentName;
    private String brand;
    private String model;
    private String serialNumber;
    private String category;
    private String type;
    private Integer qte;
    private String supplier;
    private String supplierId;
    private String shelfId;
    private String department;
    
    private LocalDate purchaseDate;
    private LocalDate warrantyExpiration;
    private Double purchasePrice;
    private String invoiceRef;
    
    // File Documents (Base64 encoded)
    private String invoiceFileName;
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String invoiceFileData;
    private String warrantyFileName;
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String warrantyFileData;
    
    private String qrCode;
    private String icon;
    private String note;
    private LocalDateTime locationChangeAt;
    private Boolean locationChanged;

    @CreatedDate
    private LocalDateTime createdAt;
    
    @LastModifiedDate
    private LocalDateTime updatedAt;
    
    private String createdBy;

    // Device Specifications
    private java.util.Map<String, String> specifications = new java.util.HashMap<>();
    private String status;
    private String assignedToEquipmentName;
    private String assignedToEquipmentId;
    private String allocatedToTechnicianName;
    private String allocatedToTechnicianId;
    private List<LifecycleEntry> lifecycle = new ArrayList<>();

    // ── IT Manager Assignment Fields ──────────────────────────────
    /** IDs of users this equipment is assigned to (IT Manager assignment) */
    private List<String> itAssignedUserIds = new ArrayList<>();

    /** Names of users this equipment is assigned to */
    private List<String> itAssignedUserNames = new ArrayList<>();

    /** Department ID if equipment is shared-assigned to a department */
    private String itAssignedDepartmentId;

    /** Department name if equipment is shared-assigned to a department */
    private String itAssignedDepartmentName;

    /** When the IT Manager performed the last assignment */
    private LocalDateTime itAssignedAt;

    /** True if the IT Manager has requested a return to stock */
    private Boolean returnRequested = false;

    /** Note explaining why the equipment is being returned */
    private String returnNote;

    /** When the return was requested */
    private LocalDateTime returnRequestedAt;

    // Constructors
    public Equipment() {}

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    
    public String getEquipmentName() { return equipmentName; }
    public void setEquipmentName(String equipmentName) { this.equipmentName = equipmentName; }
    
    public String getBrand() { return brand; }
    public void setBrand(String brand) { this.brand = brand; }
    
    public String getModel() { return model; }
    public void setModel(String model) { this.model = model; }
    
    public String getSerialNumber() { return serialNumber; }
    public void setSerialNumber(String serialNumber) { this.serialNumber = serialNumber; }
    
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    
    public Integer getQte() { return qte; }
    public void setQte(Integer qte) { this.qte = qte; }
    
    public String getSupplier() { return supplier; }
    public void setSupplier(String supplier) { this.supplier = supplier; }
    
    public String getSupplierId() { return supplierId; }
    public void setSupplierId(String supplierId) { this.supplierId = supplierId; }
    
    public String getShelfId() { return shelfId; }
    public void setShelfId(String shelfId) { this.shelfId = shelfId; }
    
    public LocalDate getPurchaseDate() { return purchaseDate; }
    public void setPurchaseDate(LocalDate purchaseDate) { this.purchaseDate = purchaseDate; }
    
    public LocalDate getWarrantyExpiration() { return warrantyExpiration; }
    public void setWarrantyExpiration(LocalDate warrantyExpiration) { this.warrantyExpiration = warrantyExpiration; }
    
    public Double getPurchasePrice() { return purchasePrice; }
    public void setPurchasePrice(Double purchasePrice) { this.purchasePrice = purchasePrice; }

    public String getInvoiceRef() { return invoiceRef; }
    public void setInvoiceRef(String invoiceRef) { this.invoiceRef = invoiceRef; }
    
    public String getInvoiceFileName() { return invoiceFileName; }
    public void setInvoiceFileName(String invoiceFileName) { this.invoiceFileName = invoiceFileName; }
    
    public String getInvoiceFileData() { return invoiceFileData; }
    public void setInvoiceFileData(String invoiceFileData) { this.invoiceFileData = invoiceFileData; }
    
    public String getWarrantyFileName() { return warrantyFileName; }
    public void setWarrantyFileName(String warrantyFileName) { this.warrantyFileName = warrantyFileName; }
    
    public String getWarrantyFileData() { return warrantyFileData; }
    public void setWarrantyFileData(String warrantyFileData) { this.warrantyFileData = warrantyFileData; }
    
    public String getQrCode() { return qrCode; }
    public void setQrCode(String qrCode) { this.qrCode = qrCode; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public String getIcon() { return icon; }
    public void setIcon(String icon) { this.icon = icon; }

    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }

    public String getDepartment() { return department; }
    public void setDepartment(String department) { this.department = department; }

    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }

    public LocalDateTime getLocationChangeAt() { return locationChangeAt; }
    public void setLocationChangeAt(LocalDateTime locationChangeAt) { this.locationChangeAt = locationChangeAt; }

    public Boolean getLocationChanged() { return locationChanged; }
    public void setLocationChanged(Boolean locationChanged) { this.locationChanged = locationChanged; }

    public java.util.Map<String, String> getSpecifications() { return specifications; }
    public void setSpecifications(java.util.Map<String, String> specifications) { this.specifications = specifications; }

    public String getAssignedToEquipmentName() { return assignedToEquipmentName; }
    public void setAssignedToEquipmentName(String assignedToEquipmentName) { this.assignedToEquipmentName = assignedToEquipmentName; }

    public String getAssignedToEquipmentId() { return assignedToEquipmentId; }
    public void setAssignedToEquipmentId(String assignedToEquipmentId) { this.assignedToEquipmentId = assignedToEquipmentId; }

    public String getAllocatedToTechnicianName() { return allocatedToTechnicianName; }
    public void setAllocatedToTechnicianName(String allocatedToTechnicianName) { this.allocatedToTechnicianName = allocatedToTechnicianName; }

    public String getAllocatedToTechnicianId() { return allocatedToTechnicianId; }
    public void setAllocatedToTechnicianId(String allocatedToTechnicianId) { this.allocatedToTechnicianId = allocatedToTechnicianId; }

    public List<LifecycleEntry> getLifecycle() { return lifecycle; }
    public void setLifecycle(List<LifecycleEntry> lifecycle) { this.lifecycle = lifecycle; }

    // ── IT Manager Assignment Getters/Setters ──────────────────────
    public List<String> getItAssignedUserIds() { return itAssignedUserIds; }
    public void setItAssignedUserIds(List<String> itAssignedUserIds) { this.itAssignedUserIds = itAssignedUserIds; }

    public List<String> getItAssignedUserNames() { return itAssignedUserNames; }
    public void setItAssignedUserNames(List<String> itAssignedUserNames) { this.itAssignedUserNames = itAssignedUserNames; }

    public String getItAssignedDepartmentId() { return itAssignedDepartmentId; }
    public void setItAssignedDepartmentId(String itAssignedDepartmentId) { this.itAssignedDepartmentId = itAssignedDepartmentId; }

    public String getItAssignedDepartmentName() { return itAssignedDepartmentName; }
    public void setItAssignedDepartmentName(String itAssignedDepartmentName) { this.itAssignedDepartmentName = itAssignedDepartmentName; }

    public LocalDateTime getItAssignedAt() { return itAssignedAt; }
    public void setItAssignedAt(LocalDateTime itAssignedAt) { this.itAssignedAt = itAssignedAt; }

    public Boolean getReturnRequested() { return returnRequested; }
    public void setReturnRequested(Boolean returnRequested) { this.returnRequested = returnRequested; }

    public String getReturnNote() { return returnNote; }
    public void setReturnNote(String returnNote) { this.returnNote = returnNote; }

    public LocalDateTime getReturnRequestedAt() { return returnRequestedAt; }
    public void setReturnRequestedAt(LocalDateTime returnRequestedAt) { this.returnRequestedAt = returnRequestedAt; }
}
