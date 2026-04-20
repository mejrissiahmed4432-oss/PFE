package com.example.usermicroservice.controller;

import com.example.usermicroservice.model.Alert;
import com.example.usermicroservice.service.AlertService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/alerts")
@CrossOrigin(origins = "http://localhost:4200")
public class AlertController {

    @Autowired
    private AlertService alertService;

    @GetMapping
    public ResponseEntity<List<Alert>> getAllAlerts(
            @RequestParam(required = false) String userId,
            @RequestParam(required = false) String role) {
        return ResponseEntity.ok(alertService.getAllAlerts(userId, role));
    }

    @GetMapping("/unread")
    public ResponseEntity<List<Alert>> getUnreadAlerts(
            @RequestParam(required = false) String userId,
            @RequestParam(required = false) String role) {
        return ResponseEntity.ok(alertService.getUnreadAlerts(userId, role));
    }

    @PutMapping("/{id}/read")
    public ResponseEntity<Alert> markAsRead(@PathVariable String id) {
        return ResponseEntity.ok(alertService.markAsRead(id));
    }

    @PutMapping("/read-all")
    public ResponseEntity<Void> markAllAsRead(
            @RequestParam(required = false) String userId,
            @RequestParam(required = false) String role) {
        alertService.markAllAsRead(userId, role);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/internal/trigger")
    public ResponseEntity<Void> triggerWebsocket() {
        alertService.triggerWebSocketAlerts();
        return ResponseEntity.ok().build();
    }

    @PostMapping
    public ResponseEntity<Void> createAlert(@RequestBody java.util.Map<String, String> body) {
        String title    = body.getOrDefault("title", "Notification");
        String message  = body.getOrDefault("message", "");
        String type     = body.getOrDefault("type", "INFO");
        String category = body.getOrDefault("category", "TASK");
        String relatedId = body.get("relatedId");
        String recipientId = body.get("recipientId");
        String targetRole = body.get("targetRole");
        
        System.out.println("[AlertController] Received internal alert request:");
        System.out.println("  Title: " + title);
        System.out.println("  Recipient: " + recipientId);
        System.out.println("  Target Role: " + targetRole);
        
        alertService.createAlert(title, message, type, category, relatedId, recipientId, targetRole);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteAlert(@PathVariable String id) {
        alertService.deleteAlert(id);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/all")
    public ResponseEntity<Void> deleteAllAlerts() {
        alertService.deleteAllAlerts();
        return ResponseEntity.noContent().build();
    }

}
