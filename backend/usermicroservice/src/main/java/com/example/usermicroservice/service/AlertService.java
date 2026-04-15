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

    public void triggerWebSocketAlerts() {
        messagingTemplate.convertAndSend("/topic/alerts", "UPDATE");
    }
}
