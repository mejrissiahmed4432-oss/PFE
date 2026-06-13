package com.example.technicianmicroservice.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Global exception handler for the Technician Microservice.
 *
 * This class intercepts RuntimeExceptions thrown by Feign Client Fallbacks
 * (e.g., StockManagerClientFallback, UserClientFallback)
 * and converts them into a clean, readable JSON response for the Angular frontend,
 * instead of Spring Boot's default generic "Internal Server Error" (500).
 *
 * The Angular GlobalErrorService / interceptor reads the "error" field from the
 * JSON body and displays it in the red popup dialog.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * Handles all RuntimeExceptions — primarily those thrown by Feign fallbacks
     * when a downstream microservice (stock-manager, user-microservice) is down.
     * Returns HTTP 503 Service Unavailable with the exact message from the exception.
     */
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String, Object>> handleRuntimeException(RuntimeException ex) {
        log.error("[GlobalExceptionHandler] RuntimeException interceptée : {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(Map.of(
                "error",     ex.getMessage(),
                "service",   "technician-microservice",
                "status",    503,
                "timestamp", LocalDateTime.now().toString()
        ));
    }
}
