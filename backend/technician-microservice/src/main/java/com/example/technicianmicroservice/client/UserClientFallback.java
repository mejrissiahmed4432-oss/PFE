package com.example.technicianmicroservice.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Fallback for UserClient (notifications & alerts toward user-microservice).
 * Strategy: SILENT — notifications are non-blocking side effects.
 * If user-microservice is down, we log the failure and continue.
 * The primary operation (part request approval/rejection) must NOT be blocked.
 */
@Component
public class UserClientFallback implements UserClient {

    private static final Logger log = LoggerFactory.getLogger(UserClientFallback.class);

    @Override
    public void createNotification(Map<String, String> body) {
        log.warn("[CircuitBreaker] user-microservice is DOWN — notification could not be sent. Body: {}", body);
        // Silent fallback: do nothing, do not throw exception
    }

    @Override
    public void triggerSystemAlert(Map<String, String> body) {
        log.warn("[CircuitBreaker] user-microservice is DOWN — system alert could not be triggered. Body: {}", body);
        // Silent fallback: do nothing
    }

    @Override
    public void resolveSystemAlert(String key) {
        log.warn("[CircuitBreaker] user-microservice is DOWN — alert '{}' could not be resolved.", key);
        // Silent fallback: do nothing
    }
}
