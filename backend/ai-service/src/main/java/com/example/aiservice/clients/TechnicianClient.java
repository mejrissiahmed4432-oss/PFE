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
 * Client for the technician-microservice (port 8083).
 * Fetches part requests and maintenance data for AI processing.
 */
@Component
public class TechnicianClient {

    private static final Logger log = LoggerFactory.getLogger(TechnicianClient.class);

    private final WebClient webClient;

    public TechnicianClient(@Qualifier("technicianWebClient") WebClient webClient) {
        this.webClient = webClient;
    }

    // ── Part Requests ─────────────────────────────────────────────────────────

    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> getAllPartRequests() {
        try {
            List<Map<String, Object>> result = webClient.get()
                    .uri("/api/part-requests")
                    .retrieve()
                    .bodyToMono(List.class)
                    .block();
            return result != null ? result : Collections.emptyList();
        } catch (Exception e) {
            log.error("Failed to fetch part requests: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> getPartRequestsByStatus(String status) {
        try {
            List<Map<String, Object>> result = webClient.get()
                    .uri("/api/part-requests/status/{status}", status)
                    .retrieve()
                    .bodyToMono(List.class)
                    .block();
            return result != null ? result : Collections.emptyList();
        } catch (Exception e) {
            log.error("Failed to fetch part requests by status '{}': {}", status, e.getMessage());
            return Collections.emptyList();
        }
    }

    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> getPartRequestsByRequester(String requesterId) {
        try {
            List<Map<String, Object>> result = webClient.get()
                    .uri("/api/part-requests/requester/{id}", requesterId)
                    .retrieve()
                    .bodyToMono(List.class)
                    .block();
            return result != null ? result : Collections.emptyList();
        } catch (Exception e) {
            log.error("Failed to fetch part requests for requester {}: {}", requesterId, e.getMessage());
            return Collections.emptyList();
        }
    }
<<<<<<< HEAD
    // ── Write Operations (AI Actions) ─────────────────────────────────────────

    @SuppressWarnings("unchecked")
    public String createPartRequest(Map<String, Object> payload, String requesterId) {
        try {
            payload.put("requesterId", requesterId);
            Map<String, Object> result = webClient.post()
                    .uri("/api/part-requests")
                    .bodyValue(payload)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();
            String desc = result != null ? String.valueOf(result.getOrDefault("description", "your request")) : "your request";
            return "✅ Part request for **" + desc + "** has been submitted successfully.";
        } catch (Exception e) {
            log.error("Failed to create part request: {}", e.getMessage());
            throw new RuntimeException("Could not submit part request: " + e.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    public String updateRequestStatus(String id, String status) {
        try {
            Map<String, Object> result = webClient.put()
                    .uri(uriBuilder -> uriBuilder
                            .path("/api/part-requests/{id}/status")
                            .queryParam("status", status)
                            .build(id))
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();
            String emoji = "APPROVED".equalsIgnoreCase(status) ? "✅" : "❌";
            return emoji + " Part request has been **" + status.toLowerCase() + "** successfully.";
        } catch (Exception e) {
            log.error("Failed to update request {} status to {}: {}", id, status, e.getMessage());
            throw new RuntimeException("Could not update request status: " + e.getMessage());
        }
    }
=======
>>>>>>> my-local-work
}
