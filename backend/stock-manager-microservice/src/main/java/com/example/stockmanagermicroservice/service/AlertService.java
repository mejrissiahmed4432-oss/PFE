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

    private static final String USER_SERVICE_URL = "http://user-microservice/api/alerts";

    public void createAlert(String title, String message, String type, String category, String relatedId, String targetRole) {
        Map<String, String> body = new HashMap<>();
        body.put("title", title);
        body.put("message", message);
        body.put("type", type);
        body.put("category", category);
        body.put("relatedId", relatedId);
        body.put("targetRole", targetRole);

        try {
            restTemplate.postForEntity(USER_SERVICE_URL, body, Void.class);
        } catch (Exception e) {
            System.err.println("Failed to send alert to user-microservice: " + e.getMessage());
        }
    }

    // Automatically generate alerts for warranty expiry
    @Scheduled(cron = "0 * * * * *")
    public void generateWarrantyAlerts() {
        LocalDate today = LocalDate.now();
        LocalDate thirtyDaysFromNow = today.plusDays(30);
        
        List<Equipment> equipmentWithWarranty = equipmentRepository.findAll().stream()
                .filter(e -> e.getWarrantyExpiration() != null)
                .toList();

        for (Equipment eq : equipmentWithWarranty) {
            LocalDate expDate = eq.getWarrantyExpiration();
            
            // Check if already expired (< today)
            if (expDate.isBefore(today)) {
                // Since we don't have local repository to check if alert exists, 
                // we'll send it and let usermicroservice handle duplicates if needed, 
                // or just accept multiple. In a better design, usermicroservice would check.
                createAlert("Warranty Expired: " + eq.getEquipmentName(),
                        "Warranty for " + eq.getEquipmentName() + " (SN: " + eq.getSerialNumber() + ") expired on " + expDate + ".",
                        "ERROR", "WARRANTY", eq.getId(), "STOCK_MANAGER");
            } 
            // Else check if expiring soon (< today + 30)
            else if (expDate.isBefore(thirtyDaysFromNow)) {
                createAlert("Warranty Expiring: " + eq.getEquipmentName(),
                        "Warranty for " + eq.getEquipmentName() + " (SN: " + eq.getSerialNumber() + ") will expire soon on " + expDate + ".",
                        "WARNING", "WARRANTY", eq.getId(), "STOCK_MANAGER");
            }
        }
    }

    public void generateDemoAlert() {
        createAlert("System Pulse Check",
                "This is a real-time test alert to verify the notification system is working perfectly.",
                "INFO", "SYSTEM", null, "STOCK_MANAGER");
    }
}

