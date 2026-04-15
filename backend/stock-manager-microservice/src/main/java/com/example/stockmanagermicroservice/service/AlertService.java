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
import org.springframework.messaging.simp.SimpMessagingTemplate;

@Service
public class AlertService {

    @Autowired
    private AlertRepository alertRepository;

    @Autowired
    private EquipmentRepository equipmentRepository;

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    public List<Alert> getAllAlerts() {
        return alertRepository.findAllByOrderByCreatedAtDesc().stream()
                .filter(this::isSystemAlert)
                .toList();
    }

    public List<Alert> getUnreadAlerts() {
        return alertRepository.findByReadFalseOrderByCreatedAtDesc().stream()
                .filter(this::isSystemAlert)
                .toList();
    }

    private boolean isSystemAlert(Alert alert) {
        String cat = alert.getCategory() != null ? alert.getCategory().toUpperCase() : "";
        // Only allow WARRANTY, STOCK, MAINTENANCE, and SYSTEM in the Alerts list
        // Explicitly exclude CRUD categories: EQUIPMENT, SHELF, SUPPLIER, CATEGORY
        return cat.equals("WARRANTY") || cat.equals("STOCK") || cat.equals("MAINTENANCE") || cat.equals("SYSTEM");
    }

    public Alert markAsRead(String id) {
        Alert alert = alertRepository.findById(id).orElseThrow();
        alert.setRead(true);
        Alert saved = alertRepository.save(alert);
        messagingTemplate.convertAndSend("/topic/alerts", "UPDATE");
        return saved;
    }

    public void createAlert(String title, String message, String type, String category, String relatedId) {
        Alert alert = new Alert(title, message, type, category, relatedId);
        alertRepository.save(alert);
        messagingTemplate.convertAndSend("/topic/alerts", "UPDATE");
    }

    public void deleteAlert(String id) {
        alertRepository.deleteById(id);
        messagingTemplate.convertAndSend("/topic/alerts", "UPDATE");
    }

    public void deleteAllAlerts() {
        alertRepository.deleteAll();
        messagingTemplate.convertAndSend("/topic/alerts", "UPDATE");
    }

    // Automatically generate alerts for warranty expiry
    //@Scheduled(cron = "0 0 0 * * *") // Every day at midnight
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
                boolean alreadyHasError = alertRepository.existsByCategoryAndRelatedIdAndType("WARRANTY", eq.getId(), "ERROR");
                if (!alreadyHasError) {
                    createAlert("Warranty Expired: " + eq.getEquipmentName(),
                            "Warranty for " + eq.getEquipmentName() + " (SN: " + eq.getSerialNumber() + ") expired on " + expDate + ".",
                            "ERROR", "WARRANTY", eq.getId());
                }
            } 
            // Else check if expiring soon (< today + 30)
            else if (expDate.isBefore(thirtyDaysFromNow)) {
                boolean alreadyHasWarning = alertRepository.existsByCategoryAndRelatedIdAndType("WARRANTY", eq.getId(), "WARNING");
                if (!alreadyHasWarning) {
                    createAlert("Warranty Expiring: " + eq.getEquipmentName(),
                            "Warranty for " + eq.getEquipmentName() + " (SN: " + eq.getSerialNumber() + ") will expire soon on " + expDate + ".",
                            "WARNING", "WARRANTY", eq.getId());
                }
            }
        }
    }

    public void generateDemoAlert() {
        createAlert("System Pulse Check",
                "This is a real-time test alert to verify the notification system is working perfectly.",
                "INFO", "SYSTEM", null);
    }

    public void updateAlertsRelatedToNameChange(String relatedId, String oldName, String newName) {
        if (oldName == null || newName == null || oldName.equals(newName)) return;
        List<Alert> alerts = alertRepository.findByRelatedId(relatedId);
        boolean changed = false;
        for (Alert alert : alerts) {
            boolean currentAlertChanged = false;
            if (alert.getTitle() != null && alert.getTitle().contains(oldName)) {
                alert.setTitle(alert.getTitle().replace(oldName, newName));
                currentAlertChanged = true;
            }
            if (alert.getMessage() != null && alert.getMessage().contains(oldName)) {
                alert.setMessage(alert.getMessage().replace(oldName, newName));
                currentAlertChanged = true;
            }
            if (currentAlertChanged) changed = true;
        }
        if (changed) {
            alertRepository.saveAll(alerts);
            messagingTemplate.convertAndSend("/topic/alerts", "UPDATE");
        }
    }

}
