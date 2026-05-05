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

    @Autowired
    private org.springframework.data.mongodb.core.MongoTemplate mongoTemplate;
    
    @GetMapping
    public ResponseEntity<?> getAllAlerts(
            @RequestParam(required = false) String userId,
            @RequestParam(required = false) String role) {
        try {
            return ResponseEntity.ok(alertService.getAllAlerts(userId, role));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body("Error fetching alerts: " + e.getMessage());
        }
    }

    @GetMapping("/active")
    public ResponseEntity<?> getActiveAlerts(
            @RequestParam(required = false) String userId,
            @RequestParam(required = false) String role) {
        try {
            return ResponseEntity.ok(alertService.getActiveAlerts(userId, role));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body("Error fetching active alerts: " + e.getMessage());
        }
    }

    @GetMapping("/debug/raw")
    public ResponseEntity<?> getRawAlerts() {
        try {
            return ResponseEntity.ok(mongoTemplate.findAll(java.util.Map.class, "alerts"));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(e.getMessage());
        }
    }

    @PutMapping("/{id}/resolve")
    public ResponseEntity<Alert> markAsResolved(@PathVariable String id) {
        return ResponseEntity.ok(alertService.markAsResolved(id));
    }

    @PutMapping("/resolve-all")
    public ResponseEntity<Void> markAllAsResolved(
            @RequestParam(required = false) String userId,
            @RequestParam(required = false) String role) {
        alertService.markAllAsResolved(userId, role);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/internal/trigger")
    public ResponseEntity<Void> triggerWebsocket() {
        alertService.triggerWebSocketAlerts();
        return ResponseEntity.ok().build();
    }

    // New internal robust alert endpoint
    @PostMapping("/system")
    public ResponseEntity<Void> createOrUpdateSystemAlert(@RequestBody java.util.Map<String, String> body) {
        String key = body.get("key");
        if (key == null || key.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        String type = body.getOrDefault("type", "SYSTEM");
        String priority = body.getOrDefault("priority", "MEDIUM");
        String targetType = body.getOrDefault("targetType", "ROLE");
        String targetId = body.get("targetId");
        String title = body.getOrDefault("title", "System Alert");
        String message = body.getOrDefault("message", "");
        
        System.out.println("[AlertController] System alert triggered: " + key);
        alertService.createOrUpdateAlert(key, type, priority, targetType, targetId, title, message);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/system/{key}/resolve")
    public ResponseEntity<Void> resolveSystemAlert(@PathVariable String key) {
        System.out.println("[AlertController] System alert resolved: " + key);
        alertService.resolveAlert(key);
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
