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
    // IT_MANAGER is treated as an alias for stock_manager in terms of permissions
    private static final Map<String, Set<String>> ACTION_PERMISSIONS = new java.util.HashMap<>(Map.ofEntries(
        Map.entry("ADD_EQUIPMENT",       Set.of("stock_manager", "it_manager", "admin")),
        Map.entry("UPDATE_EQUIPMENT",    Set.of("stock_manager", "it_manager", "admin")),
        Map.entry("DELETE_EQUIPMENT",    Set.of("stock_manager", "it_manager", "admin")),
        Map.entry("APPROVE_REQUEST",     Set.of("stock_manager", "it_manager", "admin")),
        Map.entry("REJECT_REQUEST",      Set.of("stock_manager", "it_manager", "admin")),
        Map.entry("SUBMIT_PART_REQUEST", Set.of("technician", "admin")),
        Map.entry("CREATE_TASK",         Set.of("stock_manager", "it_manager", "technician", "admin")),
        Map.entry("UPDATE_TASK",         Set.of("stock_manager", "it_manager", "technician", "admin")),
        Map.entry("DELETE_TASK",         Set.of("stock_manager", "it_manager", "technician", "admin")),
        Map.entry("UPDATE_TICKET",       Set.of("stock_manager", "it_manager", "technician", "admin")),
        Map.entry("DELETE_TICKET",       Set.of("stock_manager", "it_manager", "technician", "admin")),
        Map.entry("SEND_MESSAGE",        Set.of("stock_manager", "it_manager", "technician", "admin")),
        Map.entry("CREATE_CATEGORY",     Set.of("stock_manager", "it_manager", "admin")),
        Map.entry("ADD_TYPE",            Set.of("stock_manager", "it_manager", "admin")),
        Map.entry("ADD_SUPPLIER",        Set.of("stock_manager", "it_manager", "admin"))
    ));

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
     * IT_MANAGER is treated as stock_manager for all permission checks.
     */
    public boolean isAllowed(String actionType, String role) {
        String normalizedRole = normalizeRole(role);
        Set<String> allowedRoles = ACTION_PERMISSIONS.get(actionType.toUpperCase());
        if (allowedRoles == null) return false;
        return allowedRoles.contains(normalizedRole) || "admin".equals(normalizedRole);
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
                case "UPDATE_EQUIPMENT"    -> {
                    String updateId = resolveEquipmentId(payload);
                    if (updateId == null) yield "❌ Could not find equipment. Please provide a valid ID or serial number.";
                    yield stockClient.updateEquipment(updateId, payload);
                }
                case "DELETE_EQUIPMENT"    -> {
                    String deleteId = resolveEquipmentId(payload);
                    if (deleteId == null) yield "❌ Could not find equipment. Please provide a valid ID or serial number.";
                    yield stockClient.deleteEquipment(deleteId);
                }
                case "APPROVE_REQUEST"     -> technicianClient.updateRequestStatus(
                        (String) payload.get("id"), "APPROVED");
                case "REJECT_REQUEST"      -> technicianClient.updateRequestStatus(
                        (String) payload.get("id"), "REJECTED");
                case "SUBMIT_PART_REQUEST" -> technicianClient.createPartRequest(payload, userId);
                case "CREATE_TASK"         -> userClient.createTask(payload, userId);
                case "UPDATE_TASK"         -> {
                    String updateId = resolveTaskId(payload);
                    if (updateId == null) yield "❌ Could not find task by title, date, or ID.";
                    yield userClient.updateTask(updateId, payload);
                }
                case "DELETE_TASK"         -> {
                    String deleteId = resolveTaskId(payload);
                    if (deleteId == null) yield "❌ Could not find task by title, date, or ID.";
                    yield userClient.deleteTask(deleteId);
                }
                case "UPDATE_TICKET"       -> {
                    String updateId = resolveTicketId(payload);
                    if (updateId == null) yield "❌ Could not find ticket by title, date, or ID.";
                    yield userClient.updateTicket(updateId, payload);
                }
                case "DELETE_TICKET"       -> {
                    String deleteId = resolveTicketId(payload);
                    if (deleteId == null) yield "❌ Could not find ticket by title, date, or ID.";
                    yield userClient.deleteTicket(deleteId);
                }
                case "SEND_MESSAGE"        -> {
                    if (Boolean.TRUE.equals(payload.get("multiMessage"))) {
                        java.util.List<Map<String, Object>> messages = (java.util.List<Map<String, Object>>) payload.get("messages");
                        StringBuilder sb = new StringBuilder();
                        for (Map<String, Object> msg : messages) {
                            sb.append(processSingleMessage(msg)).append("\n");
                        }
                        yield "🚀 Multi-message execution:\n" + sb.toString();
                    }
                    yield processSingleMessage(payload);
                }
                case "CREATE_CATEGORY"     -> stockClient.createCategory(payload);
                case "ADD_TYPE"            -> stockClient.addTypeToCategory(
                        (String) payload.get("categoryId"), payload);
                case "ADD_SUPPLIER"        -> stockClient.createSupplier(payload);
                default -> "❌ Unknown action: " + actionType;
            };
        } catch (Exception e) {
            log.error("Action execution failed for {}: {}", actionType, e.getMessage());
            return "❌ Action failed: " + e.getMessage();
        }
    }

    /**
     * Resolves the real MongoDB document ID for an equipment item.
     * The AI may provide a serial number, an equipment name, or an actual ID.
     * This method searches all equipment to find a match.
     */
    @SuppressWarnings("unchecked")
    private String resolveEquipmentId(Map<String, Object> payload) {
        // 1. If payload already has a valid-looking MongoDB ObjectId, use it directly
        String id = (String) payload.get("id");
        if (id != null && !id.isBlank() && id.length() == 24) {
            return id; // Looks like a valid MongoDB ObjectId
        }

        // 2. Try to find by serial number
        String serial = (String) payload.get("serialNumber");
        if (serial == null && id != null) {
            // The AI might have put the serial number in the "id" field
            serial = id;
        }

        if (serial != null && !serial.isBlank()) {
            try {
                java.util.List<Map<String, Object>> allEquipment = stockClient.getAllEquipment();
                if (allEquipment != null) {
                    String searchSerial = serial;
                    for (Map<String, Object> eq : allEquipment) {
                        String eqSerial = String.valueOf(eq.getOrDefault("serialNumber", ""));
                        String eqId = String.valueOf(eq.getOrDefault("id", ""));
                        if (eqSerial.equalsIgnoreCase(searchSerial)) {
                            log.info("Resolved serial {} to equipment ID {}", searchSerial, eqId);
                            return eqId;
                        }
                    }
                    // 3. Also try matching by equipment name (fuzzy)
                    String name = (String) payload.get("equipmentName");
                    if (name != null && !name.isBlank()) {
                        for (Map<String, Object> eq : allEquipment) {
                            String eqName = String.valueOf(eq.getOrDefault("equipmentName", ""));
                            // Fuzzy matching: contains and case-insensitive
                            if (eqName.toLowerCase().contains(name.toLowerCase())) {
                                String eqId = String.valueOf(eq.getOrDefault("id", ""));
                                log.info("Resolved name '{}' to equipment ID {}", name, eqId);
                                return eqId;
                            }
                        }
                    }
                }
            } catch (Exception e) {
                log.error("Failed to resolve equipment ID from serial {}: {}", serial, e.getMessage());
            }
        }

        // 4. Fall back to whatever ID was provided (might still work)
        return id;
    }

    private String resolveTaskId(Map<String, Object> payload) {
        String id = (String) payload.get("id");
        if (id != null && !id.isBlank() && id.length() == 24) return id;

        String title = (String) payload.get("title");
        String date = (String) payload.get("dueDate");
        if ((title != null && !title.isBlank()) || (date != null && !date.isBlank())) {
            try {
                java.util.List<Map<String, Object>> tasks = userClient.getAllTasks();
                if (tasks != null) {
                    for (Map<String, Object> t : tasks) {
                        String tTitle = String.valueOf(t.getOrDefault("title", ""));
                        String tDate = String.valueOf(t.getOrDefault("dueDate", ""));
                        
                        // Exact match priority, then fuzzy contains
                        boolean matchesTitle = title == null || tTitle.equalsIgnoreCase(title) || tTitle.toLowerCase().contains(title.toLowerCase());
                        boolean matchesDate = date == null || tDate.startsWith(date);
                        
                        if (matchesTitle && matchesDate) {
                            return String.valueOf(t.get("id"));
                        }
                    }
                }
            } catch (Exception e) {
                log.error("Failed to resolve task: {}", e.getMessage());
            }
        }
        return id;
    }

    private String resolveTicketId(Map<String, Object> payload) {
        String id = (String) payload.get("id");
        if (id != null && !id.isBlank() && id.length() == 24) return id;

        String title = (String) payload.get("title");
        String date = (String) payload.get("createdAt");
        if ((title != null && !title.isBlank()) || (date != null && !date.isBlank())) {
            try {
                java.util.List<Map<String, Object>> tickets = userClient.getAllTickets();
                if (tickets != null) {
                    for (Map<String, Object> t : tickets) {
                        String tTitle = String.valueOf(t.getOrDefault("title", ""));
                        String tDate = String.valueOf(t.getOrDefault("createdAt", ""));
                        boolean matchesTitle = title == null || tTitle.equalsIgnoreCase(title) || tTitle.toLowerCase().contains(title.toLowerCase());
                        boolean matchesDate = date == null || tDate.startsWith(date);
                        if (matchesTitle && matchesDate) {
                            return String.valueOf(t.get("id"));
                        }
                    }
                }
            } catch (Exception e) {
                log.error("Failed to resolve ticket: {}", e.getMessage());
            }
        }
        return id;
    }

    /** Normalises role strings: STOCK_MANAGER → stock_manager, IT_MANAGER → it_manager, etc. */
    private String normalizeRole(String role) {
        return role == null ? "" : role.toLowerCase().replace(" ", "_");
    }

    /**
     * Processes a single message payload, handling role expansion if needed.
     */
    private String processSingleMessage(Map<String, Object> msg) {
        String receiverId = (String) msg.get("receiverId");
        if (receiverId != null && receiverId.startsWith("ROLE:")) {
            String targetRole = receiverId.substring(5).toUpperCase();
            log.info("Expanding broadcast for role: {}", targetRole);
            
            try {
                java.util.List<Map<String, Object>> allUsers = userClient.getAllUsers();
                int count = 0;
                for (Map<String, Object> u : allUsers) {
                    String userRole = String.valueOf(u.getOrDefault("role", ""));
                    if (userRole.equalsIgnoreCase(targetRole)) {
                        Map<String, Object> copy = new java.util.HashMap<>(msg);
                        copy.put("receiverId", u.get("id"));
                        userClient.sendMessage(copy);
                        count++;
                    }
                }
                return "📢 Broadcast sent to " + count + " users with role **" + targetRole + "**.";
            } catch (Exception e) {
                log.error("Broadcast failed: {}", e.getMessage());
                return "❌ Broadcast failed: " + e.getMessage();
            }
        }
        return userClient.sendMessage(msg);
    }
}
