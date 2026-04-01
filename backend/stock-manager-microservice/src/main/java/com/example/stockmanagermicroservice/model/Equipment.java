package com.example.stockmanagermicroservice.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDate;
import java.time.LocalDateTime;

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
    
    // File Documents (Base64 encoded)
    private String invoiceFileName;
    private String invoiceFileData;
    private String warrantyFileName;
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
    private String cpu;
    private String ram;
    private String storage;
    private String graphicsCard;
    private String operatingSystem;
    private String status;

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

    public String getCpu() { return cpu; }
    public void setCpu(String cpu) { this.cpu = cpu; }

    public String getRam() { return ram; }
    public void setRam(String ram) { this.ram = ram; }

    public String getStorage() { return storage; }
    public void setStorage(String storage) { this.storage = storage; }

    public String getGraphicsCard() { return graphicsCard; }
    public void setGraphicsCard(String graphicsCard) { this.graphicsCard = graphicsCard; }

    public String getOperatingSystem() { return operatingSystem; }
    public void setOperatingSystem(String operatingSystem) { this.operatingSystem = operatingSystem; }
}
