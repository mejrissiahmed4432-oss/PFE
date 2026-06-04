package com.example.apigateway;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Fallback controller invoked by the API Gateway Circuit Breaker when a
 * downstream microservice is unreachable or has exceeded its failure threshold.
 * Returns a structured JSON response to the Angular frontend instead of a raw error.
 */
@RestController
public class FallbackController {

    // ── Stock Manager Microservice ───────────────────────────────────────────
    @RequestMapping("/fallback/stock")
    public Mono<ResponseEntity<Map<String, Object>>> stockFallback() {
        return Mono.just(ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(Map.of(
            "error", "Le service de gestion du stock est temporairement indisponible.",
            "service", "stock-manager-microservice",
            "code", "STOCK_SERVICE_DOWN",
            "retry", true,
            "timestamp", LocalDateTime.now().toString()
        )));
    }

    // ── User Microservice ────────────────────────────────────────────────────
    @RequestMapping("/fallback/user")
    public Mono<ResponseEntity<Map<String, Object>>> userFallback() {
        return Mono.just(ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(Map.of(
            "error", "Le service d'authentification est temporairement indisponible. Veuillez réessayer dans quelques instants.",
            "service", "user-microservice",
            "code", "USER_SERVICE_DOWN",
            "retry", true,
            "timestamp", LocalDateTime.now().toString()
        )));
    }

    // ── Technician Microservice ──────────────────────────────────────────────
    @RequestMapping("/fallback/technician")
    public Mono<ResponseEntity<Map<String, Object>>> technicianFallback() {
        return Mono.just(ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(Map.of(
            "error", "Le service technicien est temporairement indisponible.",
            "service", "technician-microservice",
            "code", "TECHNICIAN_SERVICE_DOWN",
            "retry", true,
            "timestamp", LocalDateTime.now().toString()
        )));
    }

    // ── Employee Microservice ────────────────────────────────────────────────
    @RequestMapping("/fallback/employee")
    public Mono<ResponseEntity<Map<String, Object>>> employeeFallback() {
        return Mono.just(ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(Map.of(
            "error", "Le service employés est temporairement indisponible.",
            "service", "employee-microservice",
            "code", "EMPLOYEE_SERVICE_DOWN",
            "retry", true,
            "timestamp", LocalDateTime.now().toString()
        )));
    }

    // ── IT Manager Microservice ──────────────────────────────────────────────
    @RequestMapping("/fallback/it-manager")
    public Mono<ResponseEntity<Map<String, Object>>> itManagerFallback() {
        return Mono.just(ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(Map.of(
            "error", "Le service manager est temporairement indisponible.",
            "service", "it-manager-microservice",
            "code", "IT_MANAGER_SERVICE_DOWN",
            "retry", true,
            "timestamp", LocalDateTime.now().toString()
        )));
    }

    // ── AI Service ──────────────────────────────────────────────────────────
    @RequestMapping("/fallback/ai")
    public Mono<ResponseEntity<Map<String, Object>>> aiFallback() {
        return Mono.just(ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(Map.of(
            "error", "Le service IA est temporairement indisponible.",
            "service", "ai-service",
            "code", "AI_SERVICE_DOWN",
            "retry", true,
            "timestamp", LocalDateTime.now().toString()
        )));
    }
}
