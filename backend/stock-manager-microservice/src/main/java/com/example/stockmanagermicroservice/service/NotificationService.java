package com.example.stockmanagermicroservice.service;

import com.example.stockmanagermicroservice.model.Notification;
import com.example.stockmanagermicroservice.repository.NotificationRepository;
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

    public List<Notification> getAllNotifications() {
        return notificationRepository.findAllByOrderByCreatedAtDesc();
    }

    public List<Notification> getUnreadNotifications() {
        return notificationRepository.findByReadFalseOrderByCreatedAtDesc();
    }

    public Notification markAsRead(String id) {
        return notificationRepository.findById(id).map(notif -> {
            notif.setRead(true);
            Notification saved = notificationRepository.save(notif);
            messagingTemplate.convertAndSend("/topic/notifications", "UPDATE");
            return saved;
        }).orElseThrow(() -> new RuntimeException("Notification not found with id: " + id));
    }

    public void createNotification(String title, String message, String type, String category, String relatedId) {
        Notification notification = new Notification(title, message, type, category, relatedId);
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
}
