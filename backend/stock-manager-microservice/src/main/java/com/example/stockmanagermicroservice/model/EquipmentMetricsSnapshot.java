package com.example.stockmanagermicroservice.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.LocalDateTime;

@Document(collection = "equipment_metrics_history")
public class EquipmentMetricsSnapshot {
    @Id
    private String id;
    private String equipmentId;
    private String serialNumber;
    private LocalDateTime timestamp;
    
    private double cpuPercent;
    private double ramPercent;
    private double diskPercent;
    private double temperature;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getEquipmentId() { return equipmentId; }
    public void setEquipmentId(String equipmentId) { this.equipmentId = equipmentId; }

    public String getSerialNumber() { return serialNumber; }
    public void setSerialNumber(String serialNumber) { this.serialNumber = serialNumber; }

    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }

    public double getCpuPercent() { return cpuPercent; }
    public void setCpuPercent(double cpuPercent) { this.cpuPercent = cpuPercent; }

    public double getRamPercent() { return ramPercent; }
    public void setRamPercent(double ramPercent) { this.ramPercent = ramPercent; }

    public double getDiskPercent() { return diskPercent; }
    public void setDiskPercent(double diskPercent) { this.diskPercent = diskPercent; }

    public double getTemperature() { return temperature; }
    public void setTemperature(double temperature) { this.temperature = temperature; }
}
