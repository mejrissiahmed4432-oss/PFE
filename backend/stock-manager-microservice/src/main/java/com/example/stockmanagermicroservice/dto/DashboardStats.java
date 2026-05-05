package com.example.stockmanagermicroservice.dto;

import java.util.Map;
import java.util.List;
import com.example.stockmanagermicroservice.model.Equipment;

public class DashboardStats {
    private long totalEquipment;
    private long totalSuppliers;
    private long warrantyExpiringSoon; // within 30 days
    private long lowStockAlerts; // placeholder
    
    private Map<String, Long> equipmentByCategory;
    private Map<String, Long> equipmentByLocation;
    private List<Equipment> recentEquipment;

    // Getters and Setters
    public long getTotalEquipment() { return totalEquipment; }
    public void setTotalEquipment(long totalEquipment) { this.totalEquipment = totalEquipment; }

    public long getTotalSuppliers() { return totalSuppliers; }
    public void setTotalSuppliers(long totalSuppliers) { this.totalSuppliers = totalSuppliers; }

    public long getWarrantyExpiringSoon() { return warrantyExpiringSoon; }
    public void setWarrantyExpiringSoon(long warrantyExpiringSoon) { this.warrantyExpiringSoon = warrantyExpiringSoon; }

    public long getLowStockAlerts() { return lowStockAlerts; }
    public void setLowStockAlerts(long lowStockAlerts) { this.lowStockAlerts = lowStockAlerts; }

    public Map<String, Long> getEquipmentByCategory() { return equipmentByCategory; }
    public void setEquipmentByCategory(Map<String, Long> equipmentByCategory) { this.equipmentByCategory = equipmentByCategory; }

    public Map<String, Long> getEquipmentByLocation() { return equipmentByLocation; }
    public void setEquipmentByLocation(Map<String, Long> equipmentByLocation) { this.equipmentByLocation = equipmentByLocation; }

    public List<Equipment> getRecentEquipment() { return recentEquipment; }
    public void setRecentEquipment(List<Equipment> recentEquipment) { this.recentEquipment = recentEquipment; }
}
