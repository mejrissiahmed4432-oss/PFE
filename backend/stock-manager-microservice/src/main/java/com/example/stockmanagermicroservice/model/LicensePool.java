package com.example.stockmanagermicroservice.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Document(collection = "license_pools")
public class LicensePool {
    @Id
    private String id;
    
    private String softwareId; // FK to Software
    
    private LicenseModel licenseModel;
    private ActivationMethod activationMethod;
    
    private Integer totalSeats;
    private Integer availableSeats;
    
    private LocalDate expirationDate;
    private RenewalType renewalType;
    
    // Keys will be stored encrypted. The frontend will request them via a secure endpoint.
    private List<String> encryptedKeys;
    
    private String vendorSyncStatus;

    @CreatedDate
    private LocalDateTime createdAt;
    
    @LastModifiedDate
    private LocalDateTime updatedAt;

    public LicensePool() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getSoftwareId() { return softwareId; }
    public void setSoftwareId(String softwareId) { this.softwareId = softwareId; }

    public LicenseModel getLicenseModel() { return licenseModel; }
    public void setLicenseModel(LicenseModel licenseModel) { this.licenseModel = licenseModel; }

    public ActivationMethod getActivationMethod() { return activationMethod; }
    public void setActivationMethod(ActivationMethod activationMethod) { this.activationMethod = activationMethod; }

    public Integer getTotalSeats() { return totalSeats; }
    public void setTotalSeats(Integer totalSeats) { this.totalSeats = totalSeats; }

    public Integer getAvailableSeats() { return availableSeats; }
    public void setAvailableSeats(Integer availableSeats) { this.availableSeats = availableSeats; }

    public LocalDate getExpirationDate() { return expirationDate; }
    public void setExpirationDate(LocalDate expirationDate) { this.expirationDate = expirationDate; }

    public RenewalType getRenewalType() { return renewalType; }
    public void setRenewalType(RenewalType renewalType) { this.renewalType = renewalType; }

    public List<String> getEncryptedKeys() { return encryptedKeys; }
    public void setEncryptedKeys(List<String> encryptedKeys) { this.encryptedKeys = encryptedKeys; }

    public String getVendorSyncStatus() { return vendorSyncStatus; }
    public void setVendorSyncStatus(String vendorSyncStatus) { this.vendorSyncStatus = vendorSyncStatus; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
