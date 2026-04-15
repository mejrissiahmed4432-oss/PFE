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

    public List<Notification> getAllNotifications(String userId) {
        if (userId == null || userId.isEmpty()) {
            return notificationRepository.findAllByOrderByCreatedAtDesc();
        }
        return notificationRepository.findAllForUser(userId);
    }

    public List<Notification> getUnreadNotifications(String userId) {
        if (userId == null || userId.isEmpty()) {
            return notificationRepository.findByReadFalseOrderByCreatedAtDesc();
        }
        return notificationRepository.findUnreadForUser(userId);
    }

    public Notification markAsRead(String id) {
        return notificationRepository.findById(id).map(notif -> {
            notif.setRead(true);
            Notification saved = notificationRepository.save(notif);
            messagingTemplate.convertAndSend("/topic/notifications", "UPDATE");
            return saved;
        }).orElseThrow(() -> new RuntimeException("Notification not found with id: " + id));
    }

    public void createNotification(String title, String message, String type, String category, String relatedId, String recipientId) {
        Notification notification = new Notification(title, message, type, category, relatedId, recipientId);
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
