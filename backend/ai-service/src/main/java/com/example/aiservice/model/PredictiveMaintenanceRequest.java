package com.example.aiservice.model;

import java.util.List;
import java.util.Map;

public class PredictiveMaintenanceRequest {
    private String equipmentId;
    private String serialNumber;
    private String equipmentName;
    private String department;
    private List<Map<String, Object>> historicalMetrics;

    public String getEquipmentId() { return equipmentId; }
    public void setEquipmentId(String equipmentId) { this.equipmentId = equipmentId; }

    public String getSerialNumber() { return serialNumber; }
    public void setSerialNumber(String serialNumber) { this.serialNumber = serialNumber; }

    public String getEquipmentName() { return equipmentName; }
    public void setEquipmentName(String equipmentName) { this.equipmentName = equipmentName; }

    public String getDepartment() { return department; }
    public void setDepartment(String department) { this.department = department; }

    public List<Map<String, Object>> getHistoricalMetrics() { return historicalMetrics; }
    public void setHistoricalMetrics(List<Map<String, Object>> historicalMetrics) { this.historicalMetrics = historicalMetrics; }
}
