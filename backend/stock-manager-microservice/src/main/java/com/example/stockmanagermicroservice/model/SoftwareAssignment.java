package com.example.stockmanagermicroservice.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Document(collection = "software_assignments")
public class SoftwareAssignment {
    @Id
    private String id;
    
    private String licensePoolId; // FK to LicensePool
    private String softwareId; // FK to Software (denormalized for quick querying)
    
    private AssignedToType assignedToType; // USER, DEVICE, DEPARTMENT
    private String assignedTargetId; // The ID of the User or Equipment
    private String assignedTargetName; // Cached name for UI display
    
    private AssignmentStatus status; // ACTIVE, EXPIRED, REVOKED
    
    private LocalDateTime assignedAt;
    private LocalDateTime expiresAt;
    private LocalDateTime revokedAt;
    
    private String assignedBy; // Admin ID who made the assignment
    
    private String licenseKeyUsed; // If a specific key from the pool was given

    @CreatedDate
    private LocalDateTime createdAt;
    
    @LastModifiedDate
    private LocalDateTime updatedAt;

    public SoftwareAssignment() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getLicensePoolId() { return licensePoolId; }
    public void setLicensePoolId(String licensePoolId) { this.licensePoolId = licensePoolId; }

    public String getSoftwareId() { return softwareId; }
    public void setSoftwareId(String softwareId) { this.softwareId = softwareId; }

    public AssignedToType getAssignedToType() { return assignedToType; }
    public void setAssignedToType(AssignedToType assignedToType) { this.assignedToType = assignedToType; }

    public String getAssignedTargetId() { return assignedTargetId; }
    public void setAssignedTargetId(String assignedTargetId) { this.assignedTargetId = assignedTargetId; }

    public String getAssignedTargetName() { return assignedTargetName; }
    public void setAssignedTargetName(String assignedTargetName) { this.assignedTargetName = assignedTargetName; }

    public AssignmentStatus getStatus() { return status; }
    public void setStatus(AssignmentStatus status) { this.status = status; }

    public LocalDateTime getAssignedAt() { return assignedAt; }
    public void setAssignedAt(LocalDateTime assignedAt) { this.assignedAt = assignedAt; }

    public LocalDateTime getExpiresAt() { return expiresAt; }
    public void setExpiresAt(LocalDateTime expiresAt) { this.expiresAt = expiresAt; }

    public LocalDateTime getRevokedAt() { return revokedAt; }
    public void setRevokedAt(LocalDateTime revokedAt) { this.revokedAt = revokedAt; }

    public String getAssignedBy() { return assignedBy; }
    public void setAssignedBy(String assignedBy) { this.assignedBy = assignedBy; }

    public String getLicenseKeyUsed() { return licenseKeyUsed; }
    public void setLicenseKeyUsed(String licenseKeyUsed) { this.licenseKeyUsed = licenseKeyUsed; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
