package com.example.employeemicroservice.blockchain;

import com.example.employeemicroservice.model.PendingAuditLog;
import com.example.employeemicroservice.repository.PendingAuditLogRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Scheduled job that reads pending audit logs from MongoDB and
 * attempts to send them to the Blockchain (Ganache).
 * This ensures zero log loss if Ganache or the employee-microservice was down.
 */
@Component
public class AuditRetryJob {

    private static final Logger log = LoggerFactory.getLogger(AuditRetryJob.class);

    @Autowired
    private PendingAuditLogRepository pendingAuditLogRepository;

    @Autowired
    private BlockchainAuditService blockchainAuditService;

    // Runs every 5 minutes (300,000 ms)
    @Scheduled(fixedDelay = 300000)
    public void retryPendingLogs() {
        List<PendingAuditLog> pendingLogs = pendingAuditLogRepository.findAll();
        
        if (pendingLogs.isEmpty()) {
            return;
        }

        log.info("[Store & Forward] Found {} pending audit logs to process...", pendingLogs.size());

        int successCount = 0;
        for (PendingAuditLog pendingLog : pendingLogs) {
            try {
                boolean success = blockchainAuditService.retryLogToBlockchain(pendingLog);
                
                if (success) {
                    pendingAuditLogRepository.delete(pendingLog);
                    successCount++;
                }

            } catch (Exception e) {
                log.error("[Store & Forward] Failed to process pending log {}: {}", pendingLog.getId(), e.getMessage());
            }
        }

        if (successCount > 0) {
            log.info("[Store & Forward] Successfully processed {} pending logs.", successCount);
        }
    }
}
