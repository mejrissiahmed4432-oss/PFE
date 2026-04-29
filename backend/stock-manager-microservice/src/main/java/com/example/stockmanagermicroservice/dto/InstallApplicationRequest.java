package com.example.stockmanagermicroservice.dto;

public class InstallApplicationRequest {
    private String applicationId;
    private String equipmentId;
    private String installedBy;

    public InstallApplicationRequest() {}

    public String getApplicationId() { return applicationId; }
    public void setApplicationId(String applicationId) { this.applicationId = applicationId; }

    public String getEquipmentId() { return equipmentId; }
    public void setEquipmentId(String equipmentId) { this.equipmentId = equipmentId; }

    public String getInstalledBy() { return installedBy; }
    public void setInstalledBy(String installedBy) { this.installedBy = installedBy; }
}
