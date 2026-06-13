package com.example.itmanagermicroservice.client;

import com.example.itmanagermicroservice.dto.EmployeeDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

/**
 * Fallback for EmployeeClient.
 * Strategy: safe default. Return empty list when employee-microservice is down.
 */
@Component
public class EmployeeClientFallback implements EmployeeClient {

    private static final Logger log = LoggerFactory.getLogger(EmployeeClientFallback.class);

    @Override
    public List<EmployeeDTO> getAllEmployees() {
        log.error("[CircuitBreaker] employee-microservice DOWN — getAllEmployees blocked.");
        return Collections.emptyList();
    }
}
