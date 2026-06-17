package com.example.employeemicroservice.blockchain;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/employees/audit")
public class AuditController {

    @Autowired
    private BlockchainAuditService auditService;

    /**
     * Reçoit un événement d'audit depuis les autres microservices (via Feign)
     * et l'enregistre directement dans la Blockchain.
     */
    @PostMapping("/log")
    public ResponseEntity<AuditLogEntry> logEvent(@RequestBody AuditEventRequest request) {
        AuditLogEntry entry = auditService.logAction(request);
        return ResponseEntity.ok(entry);
    }

    /**
     * Retourne tous les logs lus directement depuis la Blockchain (Source de Vérité).
     */
    @GetMapping("/logs")
    public ResponseEntity<List<AuditLogEntry>> getAllLogs() {
        return ResponseEntity.ok(auditService.getAllBlockchainLogs());
    }

    /**
     * Retourne l'état de la connexion Ganache (online/offline).
     */
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getStatus() {
        boolean available = auditService.isAvailable();
        return ResponseEntity.ok(Map.of(
            "online", available,
            "network", "Local Ganache"
        ));
    }
}
