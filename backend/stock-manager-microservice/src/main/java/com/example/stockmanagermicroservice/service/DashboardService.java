package com.example.stockmanagermicroservice.service;

import com.example.stockmanagermicroservice.dto.DashboardStats;
import com.example.stockmanagermicroservice.model.Equipment;
import com.example.stockmanagermicroservice.repository.EquipmentRepository;
import com.example.stockmanagermicroservice.repository.SoftwareRepository;
import com.example.stockmanagermicroservice.repository.LicensePoolRepository;
import com.example.stockmanagermicroservice.repository.SupplierRepository;
import com.example.stockmanagermicroservice.client.UserClient;
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

    @Autowired

    private UserClient userClient;

    private SoftwareRepository softwareRepository;

    @Autowired
    private LicensePoolRepository licensePoolRepository;

    @Autowired
    private RestTemplate restTemplate;


    public DashboardStats getDashboardStats() {
        DashboardStats stats = new DashboardStats();
        
        List<Equipment> allEquipment = equipmentRepository.findAllExcludingFiles();
        
        stats.setTotalEquipment(allEquipment.size());
        stats.setTotalSuppliers(supplierRepository.count());
        stats.setTotalSoftware(softwareRepository.count());
        stats.setTotalLicenses(licensePoolRepository.count());
        
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

        // Shelf distribution
        Map<String, Long> byShelf = allEquipment.stream()
                .collect(Collectors.groupingBy(e -> e.getShelfId() != null ? e.getShelfId() : "Unknown", Collectors.counting()));
        stats.setEquipmentByLocation(byShelf);

        // Recent equipment (last 5)
        stats.setRecentEquipment(allEquipment.stream()
                .sorted((e1, e2) -> {
                    if (e1.getCreatedAt() == null) return 1;
                    if (e2.getCreatedAt() == null) return -1;
                    return e2.getCreatedAt().compareTo(e1.getCreatedAt());
                })
                .limit(5)
                .collect(Collectors.toList()));

        // Live count of active alerts for STOCK_MANAGER role
        try {
            List<?> activeAlerts = userClient.getActiveAlerts("STOCK_MANAGER");
            stats.setLowStockAlerts(activeAlerts != null ? activeAlerts.size() : 0);
        } catch (Exception e) {
            System.err.println("Failed to fetch active alerts for dashboard: " + e.getMessage());
            stats.setLowStockAlerts(0);
        }

        return stats;
    }
}
