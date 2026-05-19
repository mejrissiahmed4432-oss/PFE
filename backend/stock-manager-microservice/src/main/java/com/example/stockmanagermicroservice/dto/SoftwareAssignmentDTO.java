package com.example.stockmanagermicroservice.dto;

import com.example.stockmanagermicroservice.model.AssignedToType;
import com.example.stockmanagermicroservice.model.AssignmentStatus;

import java.time.LocalDateTime;

public class SoftwareAssignmentDTO {
    private String id;
    private String licensePoolId;
    private String softwareId;
    
    private AssignedToType assignedToType;
    private String assignedTargetId;
    private String assignedTargetName;
    
    private AssignmentStatus status;
    
    private LocalDateTime assignedAt;
    private LocalDateTime expiresAt;
    private LocalDateTime revokedAt;
    
    private String assignedBy;
    private String licenseKeyUsed;

    public SoftwareAssignmentDTO() {}

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
}
