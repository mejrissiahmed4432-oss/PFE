package com.example.stockmanagermicroservice.service;

import com.example.stockmanagermicroservice.model.Alert;
import com.example.stockmanagermicroservice.model.Equipment;
import com.example.stockmanagermicroservice.repository.AlertRepository;
import com.example.stockmanagermicroservice.repository.EquipmentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
public class AlertService {

    @Autowired
    private AlertRepository alertRepository;

    @Autowired
    private EquipmentRepository equipmentRepository;

    public List<Alert> getAllAlerts() {
        return alertRepository.findAllByOrderByCreatedAtDesc();
    }

    public List<Alert> getUnreadAlerts() {
        return alertRepository.findByReadFalseOrderByCreatedAtDesc();
    }

    public Alert markAsRead(String id) {
        Alert alert = alertRepository.findById(id).orElseThrow();
        alert.setRead(true);
        return alertRepository.save(alert);
    }

    public void createAlert(String title, String message, String type, String category, String relatedId) {
        Alert alert = new Alert(title, message, type, category, relatedId);
        alertRepository.save(alert);
    }

    // Automatically generate alerts for warranty expiry
    // @Scheduled(fixedRate = 60000) // Every 1 minute (useful for testing)
    @Scheduled(cron = "0 0 0 * * *") // Every day at midnight
    public void generateWarrantyAlerts() {
        LocalDate thirtyDaysFromNow = LocalDate.now().plusDays(30);
        List<Equipment> expiringSoon = equipmentRepository.findAll().stream()
                .filter(e -> e.getWarrantyExpiration() != null && e.getWarrantyExpiration().isBefore(thirtyDaysFromNow))
                .toList();

        for (Equipment eq : expiringSoon) {
            String title = "Warranty Expiring: " + eq.getEquipmentName();
            createAlert(title,
                    "Warranty for " + eq.getEquipmentName() + " expires on " + eq.getWarrantyExpiration(),
                    "WARNING", "WARRANTY", eq.getId());
        }
    }

    public void generateDemoAlert() {
        createAlert("System Pulse Check",
                "This is a real-time test alert to verify the notification system is working perfectly.",
                "INFO", "SYSTEM", null);
    }
}
