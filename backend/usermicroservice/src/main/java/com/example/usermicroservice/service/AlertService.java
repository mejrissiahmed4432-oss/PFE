package com.example.usermicroservice.service;

import com.example.usermicroservice.model.Alert;
import com.example.usermicroservice.repository.AlertRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import org.springframework.messaging.simp.SimpMessagingTemplate;

@Service
public class AlertService {

    @Autowired
    private AlertRepository alertRepository;

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    public List<Alert> getAllAlerts(String userId, String role) {
        if ((userId == null || userId.isEmpty()) && (role == null || role.isEmpty())) {
            return alertRepository.findAllByOrderByCreatedAtDesc().stream()
                    .filter(this::isSystemAlert)
                    .toList();
        }
        return alertRepository.findAllForUser(userId, role);
    }

    public List<Alert> getUnreadAlerts(String userId, String role) {
        if ((userId == null || userId.isEmpty()) && (role == null || role.isEmpty())) {
            return alertRepository.findByReadFalseOrderByCreatedAtDesc().stream()
                    .filter(this::isSystemAlert)
                    .toList();
        }
        return alertRepository.findUnreadForUser(userId, role);
    }

    private boolean isSystemAlert(Alert alert) {
        String cat = alert.getCategory() != null ? alert.getCategory().toUpperCase() : "";
        return cat.equals("WARRANTY") || cat.equals("STOCK") || cat.equals("MAINTENANCE") || cat.equals("SYSTEM");
    }

    public Alert markAsRead(String id) {
        Alert alert = alertRepository.findById(id).orElseThrow();
        alert.setRead(true);
        Alert saved = alertRepository.save(alert);
        messagingTemplate.convertAndSend("/topic/alerts", "UPDATE");
        return saved;
    }

    public void markAllAsRead(String userId, String role) {
        List<Alert> unread = alertRepository.findUnreadForUser(userId, role);
        unread.forEach(a -> a.setRead(true));
        alertRepository.saveAll(unread);
        messagingTemplate.convertAndSend("/topic/alerts", "UPDATE");
    }

    @Autowired
    @org.springframework.context.annotation.Lazy
    private NotificationService notificationService;

    public void createAlert(String title, String message, String type, String category, String relatedId, String recipientId, String targetRole) {
        // Enforce strict logic: Alerts are for WARNING/ERROR/URGENT only.
        if (type != null && (type.equalsIgnoreCase("INFO") || type.equalsIgnoreCase("SUCCESS"))) {
            // Reroute routine events to NotificationService
            notificationService.createNotification(title, message, type, category, relatedId, recipientId, targetRole);
            return;
        }
        
        // If it's empty, default to WARNING
        if (type == null || type.trim().isEmpty()) {
            type = "WARNING";
        }

        Alert alert = new Alert(title, message, type, category, relatedId, recipientId, targetRole);
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

    public void triggerWebSocketAlerts() {
        messagingTemplate.convertAndSend("/topic/alerts", "UPDATE");
    }
}
