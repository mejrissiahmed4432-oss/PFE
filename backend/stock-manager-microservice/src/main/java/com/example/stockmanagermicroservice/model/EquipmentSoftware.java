package com.example.stockmanagermicroservice.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.LocalDateTime;

@Document(collection = "equipment_software")
public class EquipmentSoftware {
    @Id
    private String id;
    
    private String equipmentId;
    private String osId;
    private String installedBy; // technician_id
    private LocalDateTime installedAt;
    private String status; // installed, removed

    public EquipmentSoftware() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getEquipmentId() { return equipmentId; }
    public void setEquipmentId(String equipmentId) { this.equipmentId = equipmentId; }

    public String getOsId() { return osId; }
    public void setOsId(String osId) { this.osId = osId; }

    public String getInstalledBy() { return installedBy; }
    public void setInstalledBy(String installedBy) { this.installedBy = installedBy; }

    public LocalDateTime getInstalledAt() { return installedAt; }
    public void setInstalledAt(LocalDateTime installedAt) { this.installedAt = installedAt; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
