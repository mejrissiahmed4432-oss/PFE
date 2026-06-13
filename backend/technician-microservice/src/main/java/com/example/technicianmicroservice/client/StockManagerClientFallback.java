package com.example.technicianmicroservice.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Fallback for StockManagerClient (allocate/consume parts toward stock-manager-microservice).
 * Strategy: BLOCKING — these operations modify physical inventory.
 * If stock-manager is down, we throw a RuntimeException to:
 *  1. Roll back the part request status change in the technician DB.
 *  2. Return a clean 500 error with a readable message to the UI.
 * This prevents data inconsistency (approved request but stock not updated).
 */
@Component
public class StockManagerClientFallback implements StockManagerClient {

    private static final Logger log = LoggerFactory.getLogger(StockManagerClientFallback.class);

    @Override
    public void allocateParts(Map<String, Object> requestedParts) {
        log.error("[CircuitBreaker] stock-manager-microservice is DOWN — part allocation FAILED. Request: {}", requestedParts);
        throw new RuntimeException(
            "Stock Service is not available for now! Try later or contact Admin."
        );
    }

    @Override
    public void consumeParts(String requesterId, Object consumedParts) {
        log.error("[CircuitBreaker] stock-manager-microservice is DOWN — part consumption FAILED for requester: {}", requesterId);
        throw new RuntimeException(
            "Stock Service is not available for now! Try later or contact Admin."
        );
    }
}
