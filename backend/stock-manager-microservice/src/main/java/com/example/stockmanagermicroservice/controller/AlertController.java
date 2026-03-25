package com.example.stockmanagermicroservice.controller;

import com.example.stockmanagermicroservice.model.Alert;
import com.example.stockmanagermicroservice.service.AlertService;
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

    @PostMapping("/generate-test")
    public ResponseEntity<String> generateTestAlerts() {
        alertService.generateWarrantyAlerts();
        alertService.generateDemoAlert();
        return ResponseEntity.ok("Alerts generation triggered. Check your alerts list!");
    }
}
