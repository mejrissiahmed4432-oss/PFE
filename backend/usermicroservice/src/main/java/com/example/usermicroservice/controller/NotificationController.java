package com.example.usermicroservice.controller;

import com.example.usermicroservice.model.Notification;
import com.example.usermicroservice.service.NotificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/notifications")
@CrossOrigin(origins = "http://localhost:4200")
public class NotificationController {

    @Autowired
    private NotificationService notificationService;

    @GetMapping
    public ResponseEntity<List<Notification>> getAllNotifications(@RequestParam(required = false) String userId) {
        return ResponseEntity.ok(notificationService.getAllNotifications(userId));
    }

    @GetMapping("/unread")
    public ResponseEntity<List<Notification>> getUnreadNotifications(@RequestParam(required = false) String userId) {
        return ResponseEntity.ok(notificationService.getUnreadNotifications(userId));
    }

    @PutMapping("/{id}/read")
    public ResponseEntity<Notification> markAsRead(@PathVariable String id) {
        return ResponseEntity.ok(notificationService.markAsRead(id));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteNotification(@PathVariable String id) {
        notificationService.deleteNotification(id);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/all")
    public ResponseEntity<Void> deleteAllNotifications() {
        notificationService.deleteAllNotifications();
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/internal/trigger")
    public ResponseEntity<Void> triggerWebsocket() {
        notificationService.triggerWebSocketNotifications();
        return ResponseEntity.ok().build();
    }

    @PostMapping
    public ResponseEntity<Void> createNotification(@RequestBody java.util.Map<String, String> body) {
        String title = body.get("title");
        String message = body.get("message");
        String type = body.get("type");
        String category = body.get("category");
        String relatedId = body.get("relatedId");
        String recipientId = body.get("recipientId");
        
        System.out.println("[NotificationController] Received internal notification request:");
        System.out.println("  Title: " + title);
        System.out.println("  Recipient: " + recipientId);
        System.out.println("  Category: " + category);

        notificationService.createNotification(title, message, type, category, relatedId, recipientId);
        return ResponseEntity.ok().build();
    }
}
