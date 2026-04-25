package com.example.usermicroservice.service;

import com.example.usermicroservice.model.Notification;
import com.example.usermicroservice.repository.NotificationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class NotificationService {

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @Autowired
    private MongoTemplate mongoTemplate;

    public List<Notification> getAllNotifications(String userId, String role) {
        if (isParamEmpty(userId) && isParamEmpty(role)) {
            return notificationRepository.findAllByOrderByCreatedAtDesc();
        }
        
        Criteria criteria = new Criteria().orOperator(
            Criteria.where("recipientId").is(userId),
            Criteria.where("targetRole").is(role),
            new Criteria().andOperator(
                Criteria.where("recipientId").is(null),
                Criteria.where("targetRole").is(null)
            )
        );
        
        Query query = new Query(criteria).with(org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.DESC, "createdAt"));
        return mongoTemplate.find(query, Notification.class);
    }

    public List<Notification> getUnreadNotifications(String userId, String role) {
        if (isParamEmpty(userId) && isParamEmpty(role)) {
            return notificationRepository.findByReadFalseOrderByCreatedAtDesc();
        }
        
        Criteria criteria = new Criteria().andOperator(
            Criteria.where("read").is(false),
            new Criteria().orOperator(
                Criteria.where("recipientId").is(userId),
                Criteria.where("targetRole").is(role),
                new Criteria().andOperator(
                    Criteria.where("recipientId").is(null),
                    Criteria.where("targetRole").is(null)
                )
            )
        );
        
        Query query = new Query(criteria).with(org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.DESC, "createdAt"));
        return mongoTemplate.find(query, Notification.class);
    }

    private boolean isParamEmpty(String param) {
        return param == null || param.isEmpty() || "null".equals(param) || "undefined".equals(param);
    }

    public Notification markAsRead(String id) {
        return notificationRepository.findById(id).map(notif -> {
            notif.setRead(true);
            Notification saved = notificationRepository.save(notif);
            messagingTemplate.convertAndSend("/topic/notifications", "UPDATE");
            return saved;
        }).orElseThrow(() -> new RuntimeException("Notification not found with id: " + id));
    }

    public void markAllAsRead(String userId, String role) {
        List<Notification> unread = getUnreadNotifications(userId, role);
        unread.forEach(n -> n.setRead(true));
        notificationRepository.saveAll(unread);
        messagingTemplate.convertAndSend("/topic/notifications", "UPDATE");
    }

    @Autowired
    @org.springframework.context.annotation.Lazy
    private AlertService alertService;

    public void createNotification(String title, String message, String type, String category, String relatedId, String recipientId, String targetRole) {
        // Enforce strict logic: Notifications are for INFO/SUCCESS only.
        if (type != null && (type.equalsIgnoreCase("ERROR") || type.equalsIgnoreCase("WARNING") || type.equalsIgnoreCase("URGENT"))) {
            // Reroute critical events to AlertService
            String key = "SYSTEM_ALERT_" + (relatedId != null ? relatedId : java.util.UUID.randomUUID().toString());
            alertService.createOrUpdateAlert(
                key,
                "SYSTEM",
                "HIGH",
                (targetRole != null && !targetRole.isEmpty()) ? "ROLE" : "USER",
                (targetRole != null && !targetRole.isEmpty()) ? targetRole : recipientId,
                title,
                message
            );
            return;
        }

        // Default type if missing
        if (type == null || type.trim().isEmpty()) {
            type = "INFO";
        }

        Notification notification = new Notification(title, message, type, category, relatedId, recipientId, targetRole);
        notificationRepository.save(notification);
        messagingTemplate.convertAndSend("/topic/notifications", "UPDATE");
    }

    public void deleteNotification(String id) {
        notificationRepository.deleteById(id);
        messagingTemplate.convertAndSend("/topic/notifications", "UPDATE");
    }

    public void deleteAllNotifications() {
        notificationRepository.deleteAll();
        messagingTemplate.convertAndSend("/topic/notifications", "UPDATE");
    }

    public void triggerWebSocketNotifications() {
        messagingTemplate.convertAndSend("/topic/notifications", "UPDATE");
    }
}
