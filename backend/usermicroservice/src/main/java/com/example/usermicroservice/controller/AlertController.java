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
    public ResponseEntity<List<Alert>> getAllAlerts() {
        return ResponseEntity.ok(alertService.getAllAlerts());
    }

    @GetMapping("/unread")
    public ResponseEntity<List<Alert>> getUnreadAlerts() {
        return ResponseEntity.ok(alertService.getUnreadAlerts());
    }

    @PutMapping("/{id}/read")
    public ResponseEntity<Alert> markAsRead(@PathVariable String id) {
        return ResponseEntity.ok(alertService.markAsRead(id));
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
        alertService.createAlert(title, message, type, category, relatedId);
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
