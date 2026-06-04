package com.example.itmanagermicroservice.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Fallback for UserClient in it-manager-microservice.
 * Strategy:
 *  - getAllUsers: return empty list (safe default)
 *  - state mutations (provision, update, delete): throw RuntimeException to block operation
 *  - resendInvitation: silent / exception depending on business rule. We throw exception to inform UI.
 */
@Component
public class UserClientFallback implements UserClient {

    private static final Logger log = LoggerFactory.getLogger(UserClientFallback.class);

    @Override
    public List<Map<String, Object>> getAllUsers() {
        log.error("[CircuitBreaker] user-microservice DOWN — getAllUsers blocked.");
        throw new RuntimeException("Service User is not available for now! Try later or contact Admin.");
    }

    @Override
    public Map<String, Object> provisionUser(Map<String, Object> request) {
        log.error("[CircuitBreaker] user-microservice DOWN — provisionUser failed.");
        throw new RuntimeException("Le service utilisateur est indisponible. Impossible de créer le compte.");
    }

    @Override
    public Map<String, Object> updateUserStatus(String id, Map<String, String> request) {
        log.error("[CircuitBreaker] user-microservice DOWN — updateUserStatus failed for id: {}", id);
        throw new RuntimeException("Le service utilisateur est indisponible. Impossible de modifier le statut.");
    }

    @Override
    public Map<String, Object> updateUserRole(String id, Map<String, String> request) {
        log.error("[CircuitBreaker] user-microservice DOWN — updateUserRole failed for id: {}", id);
        throw new RuntimeException("Le service utilisateur est indisponible. Impossible de modifier le rôle.");
    }

    @Override
    public Map<String, Object> resendInvitation(String id) {
        log.error("[CircuitBreaker] user-microservice DOWN — resendInvitation failed for id: {}", id);
        throw new RuntimeException("Le service utilisateur est indisponible. Impossible de renvoyer l'invitation.");
    }

    @Override
    public void deleteUser(String id) {
        log.error("[CircuitBreaker] user-microservice DOWN — deleteUser failed for id: {}", id);
        throw new RuntimeException("Le service utilisateur est indisponible. Impossible de supprimer le compte.");
    }
}
