package com.example.aiservice.client;

import com.example.aiservice.model.PendingAuditLog;
import com.example.aiservice.repository.PendingAuditLogRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import java.util.Map;

@Component
public class AuditClientFallback implements AuditClient {

    private static final Logger log = LoggerFactory.getLogger(AuditClientFallback.class);

    @Autowired
    private PendingAuditLogRepository pendingAuditLogRepository;

    @Override
    public Map<String, Object> logEvent(Map<String, Object> request) {
        log.warn("[CircuitBreaker] employee-microservice DOWN — Saving audit log to pending queue. Body: {}", request);
        try {
            PendingAuditLog pendingLog = new PendingAuditLog(
                    (String) request.get("userId"),
                    (String) request.get("userName"),
                    (String) request.get("userRole"),
                    (String) request.get("action"),
                    (String) request.get("details"),
                    (String) request.get("ipAddress")
            );
            pendingAuditLogRepository.save(pendingLog);
            log.info("[Store & Forward] Log saved to pending_audit_logs.");
        } catch (Exception e) {
            log.error("[Store & Forward] Failed to save pending log: {}", e.getMessage());
        }
        return Map.of("status", "pending", "message", "Audit log queued — employee-microservice is down");
    }
}
