package com.example.stockmanagermicroservice.dto;

public class InstallOSRequest {
    private String osId;
    private String equipmentId;
    private String installedBy;

    public InstallOSRequest() {}

    public String getOsId() { return osId; }
    public void setOsId(String osId) { this.osId = osId; }

    public String getEquipmentId() { return equipmentId; }
    public void setEquipmentId(String equipmentId) { this.equipmentId = equipmentId; }

    public String getInstalledBy() { return installedBy; }
    public void setInstalledBy(String installedBy) { this.installedBy = installedBy; }
}
