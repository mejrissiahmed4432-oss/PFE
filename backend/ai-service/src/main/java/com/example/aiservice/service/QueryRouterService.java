package com.example.aiservice.service;

import com.example.aiservice.clients.StockClient;
import com.example.aiservice.clients.TechnicianClient;
import com.example.aiservice.clients.UserClient;
import com.example.aiservice.model.QueryIntent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Routes queries to the appropriate data source based on intent.
 * Highly detailed data retrieval to ensure the AI "understands" all attributes.
 */
@Service
public class QueryRouterService {

    private static final Logger log = LoggerFactory.getLogger(QueryRouterService.class);

    private final StockClient stockClient;
    private final TechnicianClient technicianClient;
    private final UserClient userClient;
    private final RagService ragService;

    private static final Set<QueryIntent> DIRECT_QUERY_INTENTS = Set.of(
            QueryIntent.LOW_STOCK,
            QueryIntent.STOCK_STATUS,
            QueryIntent.REQUEST_STATUS,
            QueryIntent.PARTS_AVAILABILITY,
            QueryIntent.EQUIPMENT_STATUS,
            QueryIntent.SUPPLIER_INFO,
            QueryIntent.CATEGORY_INFO,
            QueryIntent.SCHEDULE_INFO,
            QueryIntent.TICKET_INFO,
            QueryIntent.NOTIFICATIONS,
            QueryIntent.ALERTS
            // EQUIPMENT_SUGGESTION uses RAG + LLM knowledge (not direct query)
    );

    private static final Map<String, String> STATUS_MAP = Map.of(
            "available", "Available",
            "disponible", "Available",
            "allocated", "Allocated",
            "assigné", "Allocated",
            "broken", "Broken",
            "cassé", "Broken",
            "panne", "Broken",
            "maintenance", "Maintenance",
            "réparation", "Maintenance"
    );

    public QueryRouterService(StockClient stockClient,
                              TechnicianClient technicianClient,
                              UserClient userClient,
                              RagService ragService) {
        this.stockClient = stockClient;
        this.technicianClient = technicianClient;
        this.userClient = userClient;
        this.ragService = ragService;
    }

    public String route(QueryIntent intent, String role, String message, String userId) {
        StringBuilder sb = new StringBuilder();
        sb.append(getSystemSnapshot());
        sb.append("\n---\n\n");

        if (DIRECT_QUERY_INTENTS.contains(intent)) {
            sb.append(directQuery(intent, role, message, userId));
        } else {
            sb.append(ragService.retrieveContext(message, role));
        }
        return sb.toString();
    }

    private String getSystemSnapshot() {
        try {
            int eq = stockClient.getAllEquipment().size();
            int su = stockClient.getAllSuppliers().size();
            int cat = stockClient.getAllCategories().size();
            int req = technicianClient.getAllPartRequests().size();
            return String.format("SYSTEM SUMMARY: %d Equipment | %d Suppliers | %d Categories | %d Requests", eq, su, cat, req);
        } catch (Exception e) {
            return "System summary unavailable.";
        }
    }

    public List<Map<String, Object>> getStructuredData(QueryIntent intent, String role, String message, String userId) {
        return switch (intent) {
            case STOCK_STATUS       -> getStockStatusData(message);
            case LOW_STOCK          -> getLowStockData(role);
            case REQUEST_STATUS     -> getRequestStatusData(role, userId);
            case PARTS_AVAILABILITY -> getPartsAvailabilityData(role, userId);
            case EQUIPMENT_STATUS   -> getEquipmentStatusData(role, userId);
            case SUPPLIER_INFO      -> getSupplierInfoData(role);
            case CATEGORY_INFO      -> getCategoryInfoData();
            case SHELF_INFO         -> getShelfInfoData();
            case TYPE_INFO          -> getTypeInfoData();
            case SCHEDULE_INFO      -> getScheduleData(role, userId);
            case TICKET_INFO        -> getTicketData(role, userId);
            case NOTIFICATIONS      -> userClient.getNotifications(userId, role);
            case ALERTS             -> userClient.getAlerts(userId, role);
            default                 -> Collections.emptyList();
        };
    }

    private String directQuery(QueryIntent intent, String role, String message, String userId) {
        return switch (intent) {
            case STOCK_STATUS       -> buildStockStatusContext(message, role, userId);
            case LOW_STOCK          -> buildLowStockContext(role);
            case REQUEST_STATUS     -> buildRequestStatusContext(role, userId);
            case PARTS_AVAILABILITY -> buildPartsAvailabilityContext(role, userId);
            case EQUIPMENT_STATUS   -> buildEquipmentStatusContext(role, userId);
            case SUPPLIER_INFO      -> buildSupplierInfoContext(role);
            case CATEGORY_INFO      -> buildCategoryInfoContext();
            case SHELF_INFO         -> buildShelfInfoContext();
            case TYPE_INFO          -> buildTypeInfoContext();
            case SCHEDULE_INFO      -> buildScheduleContext(role, userId);
            case TICKET_INFO        -> buildTicketContext(role, userId);
            case NOTIFICATIONS      -> buildNotificationsContext(userId, role);
            case ALERTS             -> buildAlertsContext(userId, role);
            default                 -> "No data context available.";
        };
    }

    // ── Supplier Info (RICH CONTEXT) ──────────────────────────────────────────

    private String buildSupplierInfoContext(String role) {
        if ("technician".equalsIgnoreCase(role)) {
            return "Access Denied: Technicians do not have access to supplier information.";
        }
        List<Map<String, Object>> suppliers = stockClient.getAllSuppliers();
        if (suppliers.isEmpty()) return "No suppliers registered.";

        StringBuilder sb = new StringBuilder("DETAILED SUPPLIER INFORMATION:\n\n");
        for (Map<String, Object> s : suppliers) {
            sb.append("• Company: ").append(s.getOrDefault("companyName", "N/A")).append("\n");
            sb.append("  Contact: ").append(s.getOrDefault("contactPerson", "N/A")).append("\n");
            sb.append("  Rating: ").append(s.getOrDefault("rating", "N/A")).append(" stars\n");
            sb.append("  Review/Note: ").append(s.getOrDefault("note", "No review provided.")).append("\n");
            sb.append("  Email: ").append(s.getOrDefault("email", "N/A")).append("\n");
            sb.append("  Phone: ").append(s.getOrDefault("phoneNumber", "N/A")).append("\n");
            sb.append("  Category: ").append(s.getOrDefault("category", "N/A")).append("\n");
            sb.append("  Address: ").append(s.getOrDefault("address", "N/A")).append("\n\n");
        }
        return sb.toString();
    }

    private List<Map<String, Object>> getSupplierInfoData(String role) {
        if ("technician".equalsIgnoreCase(role)) {
            return Collections.emptyList();
        }
        return stockClient.getAllSuppliers().stream()
                .map(s -> {
                    Map<String, Object> map = new LinkedHashMap<>();
                    map.put("company", s.getOrDefault("companyName", "N/A"));
                    map.put("rating",  s.getOrDefault("rating", "N/A"));
                    map.put("contact", s.getOrDefault("contactPerson", "N/A"));
                    return map;
                })
                .collect(Collectors.toList());
    }

    // ── Category Info (RICH CONTEXT) ──────────────────────────────────────────

    private String buildCategoryInfoContext() {
        List<Map<String, Object>> categories = stockClient.getAllCategories();
        if (categories.isEmpty()) return "No categories defined.";

        StringBuilder sb = new StringBuilder("DETAILED CATEGORY INFORMATION:\n\n");
        for (Map<String, Object> c : categories) {
            sb.append("• Category: ").append(c.getOrDefault("name", "N/A")).append("\n");
            sb.append("  Icon: ").append(c.getOrDefault("icon", "N/A")).append("\n");
            Object types = c.get("types");
            if (types instanceof List) {
                List<?> tList = (List<?>) types;
                sb.append("  Defined Types: ").append(tList.size()).append("\n");
            }
            sb.append("\n");
        }
        return sb.toString();
    }

    private List<Map<String, Object>> getCategoryInfoData() {
        return stockClient.getAllCategories().stream()
                .map(c -> {
                    Map<String, Object> map = new LinkedHashMap<>();
                    map.put("category", c.getOrDefault("name", "N/A"));
                    map.put("icon",     c.getOrDefault("icon", "N/A"));
                    return map;
                })
                .collect(Collectors.toList());
    }

    // ── Shelf Info (RICH CONTEXT) ──────────────────────────────────────────

    private String buildShelfInfoContext() {
        List<Map<String, Object>> shelves = stockClient.getAllShelves();
        if (shelves.isEmpty()) return "No shelves found in the system.";

        StringBuilder sb = new StringBuilder("SHELF INFORMATION:\n\n");
        for (Map<String, Object> s : shelves) {
            sb.append("• Shelf #").append(s.getOrDefault("nb", "N/A")).append("\n");
            sb.append("  Location: ").append(s.getOrDefault("description", "Main Warehouse")).append("\n");
            sb.append("  Capacity: ").append(s.getOrDefault("maxQte", "N/A")).append("\n");
            sb.append("  Current Qty: ").append(s.getOrDefault("currentQte", "N/A")).append("\n\n");
        }
        return sb.toString();
    }

    private List<Map<String, Object>> getShelfInfoData() {
        return stockClient.getAllShelves().stream()
                .map(s -> {
                    Map<String, Object> map = new LinkedHashMap<>();
                    map.put("shelf", s.getOrDefault("nb", "N/A"));
                    map.put("current", s.getOrDefault("currentQte", 0));
                    map.put("max", s.getOrDefault("maxQte", 0));
                    return map;
                })
                .collect(Collectors.toList());
    }

    // ── Type Info (RICH CONTEXT) ──────────────────────────────────────────

    private String buildTypeInfoContext() {
        List<Map<String, Object>> equipment = stockClient.getAllEquipment();
        Map<String, Long> typeCounts = equipment.stream()
                .collect(Collectors.groupingBy(e -> String.valueOf(e.getOrDefault("type", "Unknown")), Collectors.counting()));

        if (typeCounts.isEmpty()) return "No equipment types found.";

        StringBuilder sb = new StringBuilder("EQUIPMENT TYPE DISTRIBUTION:\n\n");
        typeCounts.forEach((type, count) -> {
            sb.append("• ").append(type).append(": ").append(count).append(" items\n");
        });
        return sb.toString();
    }

    private List<Map<String, Object>> getTypeInfoData() {
        List<Map<String, Object>> equipment = stockClient.getAllEquipment();
        Map<String, Long> typeCounts = equipment.stream()
                .collect(Collectors.groupingBy(e -> String.valueOf(e.getOrDefault("type", "Unknown")), Collectors.counting()));

        return typeCounts.entrySet().stream()
                .map(entry -> Map.<String, Object>of("type", entry.getKey(), "count", entry.getValue()))
                .collect(Collectors.toList());
    }

    // ── Rest of builders (Updated for better detail) ─────────────────────────

    private String buildStockStatusContext(String message, String role, String userId) {
        List<Map<String, Object>> all = stockClient.getAllEquipment();
        String filterStatus = detectStatus(message);
        List<Map<String, Object>> list = (filterStatus != null)
                ? all.stream().filter(e -> filterStatus.equalsIgnoreCase(String.valueOf(e.get("status")))).collect(Collectors.toList())
                : all;

        boolean isTech = "technician".equalsIgnoreCase(role);
        StringBuilder sb = new StringBuilder("EQUIPMENT STOCK OVERVIEW:\n\n");
        
        // Technicians see limited results
        int limit = isTech ? 10 : 20;
        
        list.stream().limit(limit).forEach(e -> {
            sb.append("• ").append(e.getOrDefault("equipmentName", "N/A"));
            sb.append(" [").append(e.getOrDefault("type", "N/A")).append("]");
            sb.append(" - Status: ").append(e.getOrDefault("status", "N/A"));
            
            // Techs can see OS/Specs
            if (isTech) {
                Object specs = e.get("specifications");
                if (specs instanceof Map) {
                    sb.append(" | Details: ").append(specs);
                }
            } else {
                // Stock Managers see location
                sb.append(" | Shelf: ").append(e.getOrDefault("shelfId", "N/A"));
            }
            sb.append("\n");
        });
        
        if (list.size() > limit) {
            sb.append("\n(Total items in stock: ").append(list.size()).append(". Only showing first ").append(limit).append(".)\n");
        }
        
        return sb.toString();
    }

    private List<Map<String, Object>> getStockStatusData(String message) {
        List<Map<String, Object>> all = stockClient.getAllEquipment();
        String filterStatus = detectStatus(message);
        return all.stream()
                .filter(e -> filterStatus == null || filterStatus.equalsIgnoreCase(String.valueOf(e.get("status"))))
                .map(e -> {
                    Map<String, Object> m = new HashMap<>();
                    m.put("name", e.getOrDefault("equipmentName", "N/A"));
                    m.put("status", e.getOrDefault("status", "N/A"));
                    m.put("type", e.getOrDefault("type", "N/A"));
                    return m;
                })
                .collect(Collectors.toList());
    }

    private String buildScheduleContext(String role, String userId) {
        List<Map<String, Object>> tasks = userClient.getAllTasks();
        if ("technician".equalsIgnoreCase(role) && userId != null) {
            tasks = tasks.stream()
                    .filter(t -> {
                        String assignedId = String.valueOf(t.getOrDefault("assignedTo", t.get("userId")));
                        return userId.equals(assignedId);
                    })
                    .collect(Collectors.toList());
        }
        
        if (tasks.isEmpty()) return "No scheduled tasks found.";
        
        StringBuilder sb = new StringBuilder("SCHEDULED TASKS:\n\n");
        for (Map<String, Object> t : tasks) {
            sb.append("• ").append(t.getOrDefault("title", "N/A"))
              .append(" | Priority: ").append(t.getOrDefault("priority", "N/A"))
              .append(" | Due: ").append(t.getOrDefault("dueDate", "N/A"))
              .append(" | Status: ").append(t.getOrDefault("status", "N/A"))
              .append("\n");
        }
        return sb.toString();
    }

    private List<Map<String, Object>> getScheduleData(String role, String userId) {
        List<Map<String, Object>> tasks = userClient.getAllTasks();
        if ("technician".equalsIgnoreCase(role) && userId != null) {
            tasks = tasks.stream()
                    .filter(t -> {
                        String assignedId = String.valueOf(t.getOrDefault("assignedTo", t.get("userId")));
                        return userId.equals(assignedId);
                    })
                    .collect(Collectors.toList());
        }
        return tasks.stream()
                .map(t -> Map.of("task", t.get("title"), "status", t.get("status"), "priority", t.getOrDefault("priority", "LOW")))
                .collect(Collectors.toList());
    }

    private String buildTicketContext(String role, String userId) {
        List<Map<String, Object>> tickets = userClient.getAllTickets();
        if ("technician".equalsIgnoreCase(role) && userId != null && !userId.isBlank()) {
            tickets = tickets.stream()
                    .filter(t -> {
                        String assignedId = String.valueOf(t.getOrDefault("assignedTo", t.get("userId")));
                        return userId.equals(assignedId);
                    })
                    .collect(Collectors.toList());
        }
        return "Support Tickets: " + tickets.size();
    }

    private List<Map<String, Object>> getTicketData(String role, String userId) {
        List<Map<String, Object>> tickets = userClient.getAllTickets();
        if ("technician".equalsIgnoreCase(role) && userId != null) {
            tickets = tickets.stream()
                    .filter(t -> {
                        String assignedId = String.valueOf(t.getOrDefault("assignedTo", t.get("userId")));
                        return userId.equals(assignedId);
                    })
                    .collect(Collectors.toList());
        }
        return tickets.stream().map(t -> Map.of("subject", t.get("subject"), "status", t.get("status"))).collect(Collectors.toList());
    }

    private String buildLowStockContext(String role) {
        if ("technician".equalsIgnoreCase(role)) {
            return "Access Denied: Technicians do not have access to stock replenishment levels.";
        }
        List<Map<String, Object>> low = stockClient.getAllShelves().stream()
                .filter(s -> {
                    Object c = s.get("currentQte");
                    Object m = s.get("minQte");
                    return c != null && m != null && ((Number) c).intValue() <= ((Number) m).intValue();
                }).collect(Collectors.toList());
        return "Low Stock: " + low.size() + " items.";
    }

    private List<Map<String, Object>> getLowStockData(String role) {
        if ("technician".equalsIgnoreCase(role)) return Collections.emptyList();
        return stockClient.getAllShelves().stream()
                .filter(s -> s.get("currentQte") != null)
                .map(s -> Map.of("shelf", s.get("nb"), "qty", s.get("currentQte")))
                .collect(Collectors.toList());
    }

    private String buildNotificationsContext(String userId, String role) {
        List<Map<String, Object>> notifs = userClient.getNotifications(userId, role);
        if (notifs.isEmpty()) return "No notifications.";
        
        StringBuilder sb = new StringBuilder("RECENT NOTIFICATIONS:\n\n");
        notifs.stream().limit(10).forEach(n -> 
            sb.append("• [").append(n.getOrDefault("type", "INFO")).append("] ")
              .append(n.getOrDefault("title", "Untitled")).append(": ")
              .append(n.getOrDefault("message", "")).append("\n")
        );
        return sb.toString();
    }

    private String buildAlertsContext(String userId, String role) {
        List<Map<String, Object>> alerts = userClient.getAlerts(userId, role);
        if (alerts.isEmpty()) return "No active alerts.";
        
        StringBuilder sb = new StringBuilder("ACTIVE SYSTEM ALERTS:\n\n");
        alerts.stream().limit(10).forEach(a -> 
            sb.append("• [").append(a.getOrDefault("priority", "MEDIUM")).append("] ")
              .append(a.getOrDefault("title", "Alert")).append(": ")
              .append(a.getOrDefault("message", "")).append("\n")
        );
        return sb.toString();
    }

    private String buildRequestStatusContext(String role, String userId) {
        List<Map<String, Object>> requests;
        if ("technician".equalsIgnoreCase(role) && userId != null && !userId.isBlank()) {
            requests = technicianClient.getPartRequestsByRequester(userId);
        } else {
            requests = technicianClient.getAllPartRequests();
        }
        if (requests.isEmpty()) return "No part requests found.";
        StringBuilder sb = new StringBuilder("PART REQUESTS:\n\n");
        for (Map<String, Object> r : requests) {
            sb.append("• ").append(r.getOrDefault("description", "N/A"))
              .append(" — Status: ").append(r.getOrDefault("status", "N/A"))
              .append(" [ID: ").append(r.getOrDefault("id", "N/A")).append("]\n");
        }
        return sb.toString();
    }

    private List<Map<String, Object>> getRequestStatusData(String role, String userId) {
        List<Map<String, Object>> requests;
        if ("technician".equalsIgnoreCase(role) && userId != null && !userId.isBlank()) {
            requests = technicianClient.getPartRequestsByRequester(userId);
        } else {
            requests = technicianClient.getAllPartRequests();
        }
        return requests.stream()
                .map(r -> Map.<String, Object>of(
                        "description", r.getOrDefault("description", "N/A"),
                        "status",      r.getOrDefault("status", "N/A"),
                        "id",          r.getOrDefault("id", "N/A")
                ))
                .collect(Collectors.toList());
    }

    private String buildPartsAvailabilityContext(String role, String userId) {
        return "Available Parts: " + stockClient.getEquipmentByStatus("Available").size();
    }

    private List<Map<String, Object>> getPartsAvailabilityData(String role, String userId) {
        return stockClient.getEquipmentByStatus("Available").stream()
                .map(e -> Map.of("name", e.getOrDefault("equipmentName", "N/A")))
                .collect(Collectors.toList());
    }

    private String buildEquipmentStatusContext(String role, String userId) {
        // Broken equipment is relevant to technicians
        List<Map<String, Object>> broken = stockClient.getEquipmentByStatus("Broken");
        return "Broken/Maintenance Inventory: " + broken.size() + " items needing attention.";
    }

    private List<Map<String, Object>> getEquipmentStatusData(String role, String userId) {
        return stockClient.getEquipmentByStatus("Broken").stream()
                .map(e -> Map.of("name", e.getOrDefault("equipmentName", "N/A"), "status", "Broken"))
                .collect(Collectors.toList());
    }

    private String detectStatus(String message) {
        String lower = message.toLowerCase();
        for (Map.Entry<String, String> entry : STATUS_MAP.entrySet()) {
            if (lower.contains(entry.getKey())) return entry.getValue();
        }
        return null;
    }
}
