package com.example.aiservice.service;

import com.example.aiservice.clients.StockClient;
import com.example.aiservice.clients.TechnicianClient;
import com.example.aiservice.clients.UserClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Set;

/**
 * Executes AI-triggered write actions against microservices.
 * Each action is protected by a role permission check.
 */
@Service
public class ActionExecutorService {

    private static final Logger log = LoggerFactory.getLogger(ActionExecutorService.class);

    // ── Role permission matrix ────────────────────────────────────────────────
    private static final Map<String, Set<String>> ACTION_PERMISSIONS = Map.of(
        "ADD_EQUIPMENT",       Set.of("stock_manager", "admin"),
        "UPDATE_EQUIPMENT",    Set.of("stock_manager", "admin"),
        "APPROVE_REQUEST",     Set.of("stock_manager", "admin"),
        "REJECT_REQUEST",      Set.of("stock_manager", "admin"),
        "SUBMIT_PART_REQUEST", Set.of("technician", "admin"),
        "CREATE_TASK",         Set.of("stock_manager", "technician", "admin"),
        "UPDATE_TICKET",       Set.of("stock_manager", "technician", "admin")
    );

    private final StockClient stockClient;
    private final TechnicianClient technicianClient;
    private final UserClient userClient;

    public ActionExecutorService(StockClient stockClient,
                                  TechnicianClient technicianClient,
                                  UserClient userClient) {
        this.stockClient = stockClient;
        this.technicianClient = technicianClient;
        this.userClient = userClient;
    }

    /**
     * Checks if the given role is permitted to execute the action.
     */
    public boolean isAllowed(String actionType, String role) {
        Set<String> allowedRoles = ACTION_PERMISSIONS.get(actionType.toUpperCase());
        if (allowedRoles == null) return false;
        return allowedRoles.contains(role.toLowerCase()) || "admin".equalsIgnoreCase(role);
    }

    /**
     * Executes the action and returns a human-readable result message.
     */
    public String execute(String actionType, Map<String, Object> payload, String role, String userId) {
        if (!isAllowed(actionType, role)) {
            return "❌ Access Denied: your role (**" + role + "**) is not permitted to perform this action.";
        }

        try {
            return switch (actionType.toUpperCase()) {
                case "ADD_EQUIPMENT"       -> stockClient.createEquipment(payload);
                case "UPDATE_EQUIPMENT"    -> stockClient.updateEquipment(
                        (String) payload.get("id"), payload);
                case "APPROVE_REQUEST"     -> technicianClient.updateRequestStatus(
                        (String) payload.get("id"), "APPROVED");
                case "REJECT_REQUEST"      -> technicianClient.updateRequestStatus(
                        (String) payload.get("id"), "REJECTED");
                case "SUBMIT_PART_REQUEST" -> technicianClient.createPartRequest(payload, userId);
                case "CREATE_TASK"         -> userClient.createTask(payload, userId);
                case "UPDATE_TICKET"       -> userClient.updateTicket(
                        (String) payload.get("id"), payload);
                default -> "❌ Unknown action: " + actionType;
            };
        } catch (Exception e) {
            log.error("Action execution failed for {}: {}", actionType, e.getMessage());
            return "❌ Action failed: " + e.getMessage();
        }
    }
}
