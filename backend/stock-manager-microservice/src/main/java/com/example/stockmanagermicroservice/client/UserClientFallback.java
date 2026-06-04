package com.example.stockmanagermicroservice.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Fallback for UserClient in stock-manager-microservice.
 * Strategy:
 *   - void methods (createNotification, triggerSystemAlert, resolveSystemAlert) → SILENT
 *     Notifications are side effects. If user-microservice is down, we log and continue.
 *   - getActiveAlerts() → SAFE DEFAULT: returns empty list.
 *     Prevents NullPointerExceptions in callers that iterate over the result.
 */
@Component
public class UserClientFallback implements UserClient {

    private static final Logger log = LoggerFactory.getLogger(UserClientFallback.class);

    @Override
    public void createNotification(Map<String, String> body) {
        log.warn("[CircuitBreaker] user-microservice DOWN — notification not sent. Body: {}", body);
    }

    @Override
    public void triggerSystemAlert(Map<String, String> body) {
        log.warn("[CircuitBreaker] user-microservice DOWN — system alert not triggered. Body: {}", body);
    }

    @Override
    public void resolveSystemAlert(String key) {
        log.warn("[CircuitBreaker] user-microservice DOWN — alert '{}' not resolved.", key);
    }

    @Override
    public List<?> getActiveAlerts(String role) {
        log.warn("[CircuitBreaker] user-microservice DOWN — returning empty alert list for role: {}", role);
        return Collections.emptyList();
    }
}
