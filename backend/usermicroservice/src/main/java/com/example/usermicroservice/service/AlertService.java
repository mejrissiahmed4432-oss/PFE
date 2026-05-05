package com.example.usermicroservice.service;

import com.example.usermicroservice.model.Alert;
import com.example.usermicroservice.repository.AlertRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

@Service
public class AlertService {

    @Autowired
    private AlertRepository alertRepository;

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @Autowired
    private MongoTemplate mongoTemplate;

    @Autowired
    @org.springframework.context.annotation.Lazy
    private NotificationService notificationService;

    // Cooldown in minutes (configurable later if needed)
    private static final long ALERT_COOLDOWN_MINUTES = 60;

    public List<Alert> getAllAlerts(String userId, String role) {
        if (isParamEmpty(userId) && isParamEmpty(role)) {
            return alertRepository.findAllByOrderByCreatedAtDesc();
        }
        
        Criteria criteria = new Criteria().orOperator(
            Criteria.where("targetType").is("USER").and("targetId").is(userId),
            Criteria.where("targetType").is("ROLE").and("targetId").is(role),
            Criteria.where("targetType").is(null),
            Criteria.where("targetType").exists(false)
        );
        
        Query query = new Query(criteria).with(org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.DESC, "createdAt"));
        return mongoTemplate.find(query, Alert.class);
    }

    public List<Alert> getActiveAlerts(String userId, String role) {
        if (isParamEmpty(userId) && isParamEmpty(role)) {
            return alertRepository.findByStatusOrderByCreatedAtDesc("ACTIVE");
        }
        
        Criteria criteria = new Criteria().andOperator(
            Criteria.where("status").is("ACTIVE"),
            new Criteria().orOperator(
                Criteria.where("targetType").is("USER").and("targetId").is(userId),
                Criteria.where("targetType").is("ROLE").and("targetId").is(role),
                Criteria.where("targetType").is(null),
                Criteria.where("targetType").exists(false)
            )
        );
        
        Query query = new Query(criteria).with(org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.DESC, "createdAt"));
        return mongoTemplate.find(query, Alert.class);
    }

    private boolean isParamEmpty(String param) {
        return param == null || param.isEmpty() || "null".equals(param) || "undefined".equals(param);
    }

    /**
     * Core robust alert generation with deduplication and cooldown.
     */
    public void createOrUpdateAlert(String key, String type, String priority, String targetType, String targetId, String title, String message) {
        alertRepository.findByKeyAndStatus(key, "ACTIVE").ifPresentOrElse(
            existing -> {
                // Check cooldown to avoid spam
                if (existing.getLastSentAt() != null && 
                    existing.getLastSentAt().isAfter(LocalDateTime.now().minusMinutes(ALERT_COOLDOWN_MINUTES))) {
                    return;
                }
                existing.setTitle(title);
                existing.setMessage(message);
                existing.setPriority(priority);
                existing.setLastSentAt(LocalDateTime.now());
                alertRepository.save(existing);
                messagingTemplate.convertAndSend("/topic/alerts", "UPDATE");
            },
            () -> {
                Alert alert = new Alert(key, title, message, type, priority, targetType, targetId);
                alertRepository.save(alert);
                messagingTemplate.convertAndSend("/topic/alerts", "UPDATE");
            }
        );
    }

    /**
     * Resolves an alert automatically when the condition is fixed.
     */
    public void resolveAlert(String key) {
        alertRepository.findByKeyAndStatus(key, "ACTIVE").ifPresent(a -> {
            a.setStatus("RESOLVED");
            a.setResolvedAt(LocalDateTime.now());
            alertRepository.save(a);
            messagingTemplate.convertAndSend("/topic/alerts", "UPDATE");
        });
    }

    /**
     * Manually mark an alert as resolved (e.g., from UI).
     */
    public Alert markAsResolved(String id) {
        return alertRepository.findById(id).map(a -> {
            a.setStatus("RESOLVED");
            a.setResolvedAt(LocalDateTime.now());
            Alert saved = alertRepository.save(a);
            messagingTemplate.convertAndSend("/topic/alerts", "UPDATE");
            return saved;
        }).orElseThrow(() -> new RuntimeException("Alert not found with id: " + id));
    }

    public void markAllAsResolved(String userId, String role) {
        List<Alert> active;
        if (isParamEmpty(userId) && isParamEmpty(role)) {
            active = alertRepository.findByStatusOrderByCreatedAtDesc("ACTIVE");
        } else {
            active = getActiveAlerts(userId, role);
        }
        
        active.forEach(a -> {
            a.setStatus("RESOLVED");
            a.setResolvedAt(LocalDateTime.now());
        });
        alertRepository.saveAll(active);
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

    private void pushToWebSocket(Alert alert) {
        // Send a generic UPDATE ping
        messagingTemplate.convertAndSend("/topic/alerts", "UPDATE");
        
        // Example of sending full payload to a specific user or role topic
        // If TargetType is USER: /user/{userId}/queue/alerts
        // If TargetType is ROLE: /topic/alerts/{roleName}
        // For now, sending full payload to a general topic as requested:
        // "Send full alert payload... Use user-specific topics"
        
        if ("USER".equals(alert.getTargetType()) && alert.getTargetId() != null) {
            messagingTemplate.convertAndSendToUser(alert.getTargetId(), "/queue/alerts", new WsAlertPayload("NEW", alert));
        } else if ("ROLE".equals(alert.getTargetType()) && alert.getTargetId() != null) {
             messagingTemplate.convertAndSend("/topic/alerts/role/" + alert.getTargetId(), new WsAlertPayload("NEW", alert));
        }
    }

    public void triggerWebSocketAlerts() {
        messagingTemplate.convertAndSend("/topic/alerts", "UPDATE");
    }

    // Helper class for WebSocket payload
    public static class WsAlertPayload {
        public String action;
        public Alert alert;
        
        public WsAlertPayload(String action, Alert alert) {
            this.action = action;
            this.alert = alert;
        }
    }
}
