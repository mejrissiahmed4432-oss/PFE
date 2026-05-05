package com.example.stockmanagermicroservice.service;

import com.example.stockmanagermicroservice.model.Equipment;
import com.example.stockmanagermicroservice.repository.EquipmentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class AlertService {

    @Autowired
    private EquipmentRepository equipmentRepository;

    @Autowired
    private RestTemplate restTemplate;

    private static final String USER_SERVICE_SYSTEM_ALERT_URL = "http://user-microservice/api/alerts/system";
    private static final String USER_SERVICE_RESOLVE_ALERT_URL = "http://user-microservice/api/alerts/system/{key}/resolve";

    public void triggerSystemAlert(String key, String type, String priority, String targetType, String targetId, String title, String message) {
        Map<String, String> body = new HashMap<>();
        body.put("key", key);
        body.put("type", type);
        body.put("priority", priority);
        body.put("targetType", targetType);
        if (targetId != null) body.put("targetId", targetId);
        body.put("title", title);
        body.put("message", message);

        try {
            restTemplate.postForEntity(USER_SERVICE_SYSTEM_ALERT_URL, body, Void.class);
        } catch (Exception e) {
            System.err.println("Failed to send system alert to user-microservice: " + e.getMessage());
        }
    }

    public void resolveSystemAlert(String key) {
        try {
            restTemplate.postForEntity(USER_SERVICE_RESOLVE_ALERT_URL, null, Void.class, key);
        } catch (Exception e) {
            System.err.println("Failed to resolve system alert in user-microservice: " + e.getMessage());
        }
    }

    // Automatically generate alerts for warranty expiry
    @Scheduled(cron = "0 0 * * * *") // Run every hour
    public void generateWarrantyAlerts() {
        LocalDate today = LocalDate.now();
        LocalDate thirtyDaysFromNow = today.plusDays(30);
        
        List<Equipment> equipmentWithWarranty = equipmentRepository.findAll().stream()
                .filter(e -> e.getWarrantyExpiration() != null)
                .toList();

        for (Equipment eq : equipmentWithWarranty) {
            LocalDate expDate = eq.getWarrantyExpiration();
            String expiredKey = "WARRANTY_EXPIRED_" + eq.getId();
            String expiringKey = "WARRANTY_EXPIRING_" + eq.getId();
            
            if (expDate.isBefore(today)) {
                // Already expired
                triggerSystemAlert(
                    expiredKey,
                    "WARRANTY_EXPIRED",
                    "HIGH",
                    "ROLE",
                    "STOCK_MANAGER",
                    "Warranty Expired: " + eq.getEquipmentName(),
                    "Warranty for " + eq.getEquipmentName() + " (SN: " + eq.getSerialNumber() + ") expired on " + expDate + "."
                );
                // Resolve expiring alert if it exists
                resolveSystemAlert(expiringKey);
            } 
            else if (expDate.isBefore(thirtyDaysFromNow)) {
                // Expiring soon
                triggerSystemAlert(
                    expiringKey,
                    "WARRANTY_EXPIRING",
                    "MEDIUM",
                    "ROLE",
                    "STOCK_MANAGER",
                    "Warranty Expiring: " + eq.getEquipmentName(),
                    "Warranty for " + eq.getEquipmentName() + " (SN: " + eq.getSerialNumber() + ") will expire soon on " + expDate + "."
                );
            } else {
                // Warranty extended or not close to expiry, resolve any existing alerts
                resolveSystemAlert(expiredKey);
                resolveSystemAlert(expiringKey);
            }
        }
    }
}
