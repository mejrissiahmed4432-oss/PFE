package com.example.usermicroservice.service;

import com.example.usermicroservice.model.Notification;
import com.example.usermicroservice.repository.NotificationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class NotificationService {

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    public List<Notification> getAllNotifications(String userId, String role) {
        if ((userId == null || userId.isEmpty()) && (role == null || role.isEmpty())) {
            return notificationRepository.findAllByOrderByCreatedAtDesc();
        }
        return notificationRepository.findAllForUser(userId, role);
    }

    public List<Notification> getUnreadNotifications(String userId, String role) {
        if ((userId == null || userId.isEmpty()) && (role == null || role.isEmpty())) {
            return notificationRepository.findByReadFalseOrderByCreatedAtDesc();
        }
        return notificationRepository.findUnreadForUser(userId, role);
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
        List<Notification> unread = notificationRepository.findUnreadForUser(userId, role);
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
            alertService.createAlert(title, message, type, category, relatedId, recipientId, targetRole);
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
