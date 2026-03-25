package com.example.stockmanagermicroservice.service;

import com.example.stockmanagermicroservice.dto.DashboardStats;
import com.example.stockmanagermicroservice.model.Equipment;
import com.example.stockmanagermicroservice.repository.EquipmentRepository;
import com.example.stockmanagermicroservice.repository.SupplierRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class DashboardService {

    @Autowired
    private EquipmentRepository equipmentRepository;

    @Autowired
    private SupplierRepository supplierRepository;

    public DashboardStats getDashboardStats() {
        DashboardStats stats = new DashboardStats();
        
        List<Equipment> allEquipment = equipmentRepository.findAll();
        
        stats.setTotalEquipment(allEquipment.size());
        stats.setTotalSuppliers(supplierRepository.count());
        
        // Warranty alert (within 30 days)
        LocalDate thirtyDaysFromNow = LocalDate.now().plusDays(30);
        long expiringCount = allEquipment.stream()
                .filter(e -> e.getWarrantyExpiration() != null && e.getWarrantyExpiration().isBefore(thirtyDaysFromNow))
                .count();
        stats.setWarrantyExpiringSoon(expiringCount);

        // Category distribution
        Map<String, Long> byCategory = allEquipment.stream()
                .collect(Collectors.groupingBy(e -> e.getCategory() != null ? e.getCategory() : "Uncategorized", Collectors.counting()));
        stats.setEquipmentByCategory(byCategory);

        // Location distribution
        Map<String, Long> byLocation = allEquipment.stream()
                .collect(Collectors.groupingBy(e -> e.getLocation() != null ? e.getLocation() : "Unknown", Collectors.counting()));
        stats.setEquipmentByLocation(byLocation);

        // Recent equipment (last 5)
        stats.setRecentEquipment(allEquipment.stream()
                .sorted((e1, e2) -> {
                    if (e1.getCreatedAt() == null) return 1;
                    if (e2.getCreatedAt() == null) return -1;
                    return e2.getCreatedAt().compareTo(e1.getCreatedAt());
                })
                .limit(5)
                .collect(Collectors.toList()));

        // Placeholder for low stock (not implemented in model yet)
        stats.setLowStockAlerts(2); 

        return stats;
    }
}
