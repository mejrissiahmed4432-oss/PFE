package com.example.stockmanagermicroservice.dto;

import com.example.stockmanagermicroservice.model.ActivationMethod;
import com.example.stockmanagermicroservice.model.LicenseModel;
import com.example.stockmanagermicroservice.model.RenewalType;

import java.time.LocalDate;
import java.util.List;

public class LicensePoolDTO {
    private String id;
    private String softwareId;
    
    private LicenseModel licenseModel;
    private ActivationMethod activationMethod;
    
    private Integer totalSeats;
    private Integer availableSeats;
    
    private LocalDate expirationDate;
    private RenewalType renewalType;
    
    // Unencrypted keys sent from client during creation.
    // Encrypted keys will NOT be sent back here normally.
    private List<String> rawKeys;
    
    private String vendorSyncStatus;

    public LicensePoolDTO() {}

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

    public List<String> getRawKeys() { return rawKeys; }
    public void setRawKeys(List<String> rawKeys) { this.rawKeys = rawKeys; }

    public String getVendorSyncStatus() { return vendorSyncStatus; }
    public void setVendorSyncStatus(String vendorSyncStatus) { this.vendorSyncStatus = vendorSyncStatus; }
}
