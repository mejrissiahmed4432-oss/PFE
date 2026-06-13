package com.example.stockmanagermicroservice.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class AiClientFallback implements AiClient {

    private static final Logger log = LoggerFactory.getLogger(AiClientFallback.class);

    @Override
    public Map<String, Object> predictMaintenance(Map<String, Object> request) {
        log.warn("[CircuitBreaker] ai-service is DOWN — Predictive maintenance could not be calculated. Body: {}", request);
        // Return a silent fallback response indicating the failure safely
        return Map.of(
                "status", "error",
                "message", "AI prediction ignored due to ai-service being down",
                "predictedHealthScore", 100, // Safe default
                "predictedIssues", "N/A (AI Service Offline)",
                "recommendedActions", "N/A"
        );
    }
}
