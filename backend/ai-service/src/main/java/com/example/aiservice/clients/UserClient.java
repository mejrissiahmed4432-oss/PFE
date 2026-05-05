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
 * Client for the user-microservice (port 8080).
 * Fetches tasks and tickets for AI processing.
 */
@Component
public class UserClient {

    private static final Logger log = LoggerFactory.getLogger(UserClient.class);

    private final WebClient webClient;

    public UserClient(@Qualifier("userWebClient") WebClient webClient) {
        this.webClient = webClient;
    }

    // ── Tasks / Schedule ──────────────────────────────────────────────────────

    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> getAllTasks() {
        try {
            List<Map<String, Object>> result = webClient.get()
                    .uri("/api/users/tasks")
                    .retrieve()
                    .bodyToMono(List.class)
                    .block();
            return result != null ? result : Collections.emptyList();
        } catch (Exception e) {
            log.error("Failed to fetch all tasks: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    // ── Tickets ───────────────────────────────────────────────────────────────

    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> getAllTickets() {
        try {
            List<Map<String, Object>> result = webClient.get()
                    .uri("/api/users/tickets")
                    .retrieve()
                    .bodyToMono(List.class)
                    .block();
            return result != null ? result : Collections.emptyList();
        } catch (Exception e) {
            log.error("Failed to fetch all tickets: {}", e.getMessage());
            return Collections.emptyList();
        }
    }
<<<<<<< HEAD
    // ── Write Operations (AI Actions) ─────────────────────────────────────────

    @SuppressWarnings("unchecked")
    public String createTask(Map<String, Object> payload, String userId) {
        try {
            payload.put("userId", userId);
            Map<String, Object> result = webClient.post()
                    .uri("/api/users/tasks")
                    .bodyValue(payload)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();
            String title = result != null ? String.valueOf(result.getOrDefault("title", "task")) : "task";
            return "✅ Task **" + title + "** has been created successfully.";
        } catch (Exception e) {
            log.error("Failed to create task: {}", e.getMessage());
            throw new RuntimeException("Could not create task: " + e.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    public String updateTicket(String id, Map<String, Object> payload) {
        try {
            Map<String, Object> result = webClient.put()
                    .uri("/api/users/tickets/{id}", id)
                    .bodyValue(payload)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();
            String title = result != null ? String.valueOf(result.getOrDefault("title", "ticket")) : "ticket";
            return "✅ Ticket **" + title + "** has been updated.";
        } catch (Exception e) {
            log.error("Failed to update ticket {}: {}", id, e.getMessage());
            throw new RuntimeException("Could not update ticket: " + e.getMessage());
        }
    }
=======
>>>>>>> my-local-work
}
