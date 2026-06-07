package com.example.itmanagermicroservice.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Fallback for TaskClient in it-manager-microservice.
 * State mutations (assign, update, delete) throw RuntimeException to block operation.
 * Read-only calls return empty collections.
 */
@Component
public class TaskClientFallback implements TaskClient {

    private static final Logger log = LoggerFactory.getLogger(TaskClientFallback.class);

    @Override
    public Map<String, Object> assignTask(Map<String, Object> request) {
        log.error("[CircuitBreaker] user-microservice DOWN — assignTask failed.");
        throw new RuntimeException("Le service utilisateur est indisponible. Impossible d'assigner la tâche.");
    }

    @Override
    public List<Map<String, Object>> getAllTasks() {
        log.error("[CircuitBreaker] user-microservice DOWN — getAllTasks blocked.");
        return Collections.emptyList();
    }

    @Override
    public List<Map<String, Object>> getTasksAssignedByManager(String managerId) {
        log.error("[CircuitBreaker] user-microservice DOWN — getTasksAssignedByManager blocked.");
        return Collections.emptyList();
    }

    @Override
    public List<Map<String, Object>> getTasksAssignedToUser(String userId) {
        log.error("[CircuitBreaker] user-microservice DOWN — getTasksAssignedToUser blocked.");
        return Collections.emptyList();
    }

    @Override
    public Map<String, Object> updateTask(String id, Map<String, Object> task) {
        log.error("[CircuitBreaker] user-microservice DOWN — updateTask failed for id: {}", id);
        throw new RuntimeException("Le service utilisateur est indisponible. Impossible de modifier la tâche.");
    }

    @Override
    public Map<String, Object> updateTaskStatus(String id, String status) {
        log.error("[CircuitBreaker] user-microservice DOWN — updateTaskStatus failed for id: {}", id);
        throw new RuntimeException("Le service utilisateur est indisponible. Impossible de mettre à jour le statut.");
    }

    @Override
    public void deleteTask(String id) {
        log.error("[CircuitBreaker] user-microservice DOWN — deleteTask failed for id: {}", id);
        throw new RuntimeException("Le service utilisateur est indisponible. Impossible de supprimer la tâche.");
    }
}
