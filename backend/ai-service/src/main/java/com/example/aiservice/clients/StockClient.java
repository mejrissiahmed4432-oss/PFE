package com.example.aiservice.clients;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Client for the stock-manager-microservice (port 8081).
 * Fetches equipment, shelves, and supplier data for AI processing.
 */
@Component
public class StockClient {

    private static final Logger log = LoggerFactory.getLogger(StockClient.class);

    private final WebClient webClient;

    public StockClient(@Qualifier("stockWebClient") WebClient webClient) {
        this.webClient = webClient;
    }

    // ── Equipment ─────────────────────────────────────────────────────────────

    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> getAllEquipment() {
        try {
            List<Map<String, Object>> result = webClient.get()
                    .uri("/api/equipment")
                    .retrieve()
                    .bodyToMono(List.class)
                    .block();
            return result != null ? result : Collections.emptyList();
        } catch (Exception e) {
            log.error("Failed to fetch all equipment: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> getEquipmentByStatus(String status) {
        try {
            List<Map<String, Object>> result = webClient.get()
                    .uri("/api/equipment/status/{status}", status)
                    .retrieve()
                    .bodyToMono(List.class)
                    .block();
            return result != null ? result : Collections.emptyList();
        } catch (Exception e) {
            log.error("Failed to fetch equipment by status '{}': {}", status, e.getMessage());
            return Collections.emptyList();
        }
    }

    // ── Shelves ───────────────────────────────────────────────────────────────

    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> getAllShelves() {
        try {
            List<Map<String, Object>> result = webClient.get()
                    .uri("/api/shelves")
                    .retrieve()
                    .bodyToMono(List.class)
                    .block();
            return result != null ? result : Collections.emptyList();
        } catch (Exception e) {
            log.error("Failed to fetch shelves: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    // ── Suppliers ─────────────────────────────────────────────────────────────

    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> getAllSuppliers() {
        try {
            List<Map<String, Object>> result = webClient.get()
                    .uri("/api/suppliers")
                    .retrieve()
                    .bodyToMono(List.class)
                    .block();
            return result != null ? result : Collections.emptyList();
        } catch (Exception e) {
            log.error("Failed to fetch suppliers: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    // ── Categories ────────────────────────────────────────────────────────────

    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> getAllCategories() {
        try {
            List<Map<String, Object>> result = webClient.get()
                    .uri("/api/equipment-categories")
                    .retrieve()
                    .bodyToMono(List.class)
                    .block();
            return result != null ? result : Collections.emptyList();
        } catch (Exception e) {
            log.error("Failed to fetch categories: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    // ── Dashboard summary ─────────────────────────────────────────────────────

    @SuppressWarnings("unchecked")
    public Map<String, Object> getDashboardSummary() {
        try {
            Map<String, Object> result = webClient.get()
                    .uri("/api/dashboard/stats")
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();
            return result != null ? result : Collections.emptyMap();
        } catch (Exception e) {
            log.error("Failed to fetch dashboard summary: {}", e.getMessage());
            return Collections.emptyMap();
        }
    }
    // ── Write Operations (AI Actions) ─────────────────────────────────────────

    @SuppressWarnings("unchecked")
    public String createEquipment(Map<String, Object> payload) {
        try {
            Map<String, Object> result = webClient.post()
                    .uri("/api/equipment")
                    .bodyValue(payload)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();
            String name = result != null ? String.valueOf(result.getOrDefault("equipmentName", "Unknown")) : "Unknown";
            return "✅ Equipment **" + name + "** has been successfully added to inventory.";
        } catch (Exception e) {
            log.error("Failed to create equipment: {}", e.getMessage());
            throw new RuntimeException("Could not create equipment: " + e.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    public String updateEquipment(String id, Map<String, Object> payload) {
        try {
            Map<String, Object> result = webClient.put()
                    .uri("/api/equipment/{id}", id)
                    .bodyValue(payload)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();
            String name = result != null ? String.valueOf(result.getOrDefault("equipmentName", "Unknown")) : "Unknown";
            return "✅ Equipment **" + name + "** has been successfully updated.";
        } catch (Exception e) {
            log.error("Failed to update equipment {}: {}", id, e.getMessage());
            throw new RuntimeException("Could not update equipment: " + e.getMessage());
        }
    }
}
