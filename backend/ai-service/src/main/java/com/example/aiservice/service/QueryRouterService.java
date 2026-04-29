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
            QueryIntent.TICKET_INFO
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
            case LOW_STOCK          -> getLowStockData();
            case REQUEST_STATUS     -> getRequestStatusData(role, userId);
            case PARTS_AVAILABILITY -> getPartsAvailabilityData();
            case EQUIPMENT_STATUS   -> getEquipmentStatusData();
            case SUPPLIER_INFO      -> getSupplierInfoData();
            case CATEGORY_INFO      -> getCategoryInfoData();
            case SCHEDULE_INFO      -> getScheduleData();
            case TICKET_INFO        -> getTicketData();
            default                 -> Collections.emptyList();
        };
    }

    private String directQuery(QueryIntent intent, String role, String message, String userId) {
        return switch (intent) {
            case STOCK_STATUS       -> buildStockStatusContext(message);
            case LOW_STOCK          -> buildLowStockContext();
            case REQUEST_STATUS     -> buildRequestStatusContext(role, userId);
            case PARTS_AVAILABILITY -> buildPartsAvailabilityContext();
            case EQUIPMENT_STATUS   -> buildEquipmentStatusContext();
            case SUPPLIER_INFO      -> buildSupplierInfoContext();
            case CATEGORY_INFO      -> buildCategoryInfoContext();
            case SCHEDULE_INFO      -> buildScheduleContext();
            case TICKET_INFO        -> buildTicketContext();
            default                 -> "No data context available.";
        };
    }

    // ── Supplier Info (RICH CONTEXT) ──────────────────────────────────────────

    private String buildSupplierInfoContext() {
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

    private List<Map<String, Object>> getSupplierInfoData() {
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

    // ── Rest of builders (Updated for better detail) ─────────────────────────

    private String buildStockStatusContext(String message) {
        List<Map<String, Object>> all = stockClient.getAllEquipment();
        String filterStatus = detectStatus(message);
        List<Map<String, Object>> list = (filterStatus != null)
                ? all.stream().filter(e -> filterStatus.equalsIgnoreCase(String.valueOf(e.get("status")))).collect(Collectors.toList())
                : all;

        StringBuilder sb = new StringBuilder("EQUIPMENT STATUS OVERVIEW:\n\n");
        list.stream().limit(15).forEach(e -> 
            sb.append("• ").append(e.getOrDefault("equipmentName", "N/A"))
              .append(" [SN: ").append(e.getOrDefault("serialNumber", "N/A")).append("]")
              .append(" - Status: ").append(e.getOrDefault("status", "N/A")).append("\n")
        );
        return sb.toString();
    }

    private List<Map<String, Object>> getStockStatusData(String message) {
        List<Map<String, Object>> all = stockClient.getAllEquipment();
        String filterStatus = detectStatus(message);
        return all.stream()
                .filter(e -> filterStatus == null || filterStatus.equalsIgnoreCase(String.valueOf(e.get("status"))))
                .map(e -> Map.of("name", e.getOrDefault("equipmentName", "N/A"), "status", e.getOrDefault("status", "N/A")))
                .collect(Collectors.toList());
    }

    private String buildScheduleContext() {
        List<Map<String, Object>> tasks = userClient.getAllTasks();
        return "Schedule Tasks: " + tasks.stream().map(t -> String.valueOf(t.get("title"))).collect(Collectors.joining(", "));
    }

    private List<Map<String, Object>> getScheduleData() {
        return userClient.getAllTasks().stream().map(t -> Map.of("task", t.get("title"), "status", t.get("status"))).collect(Collectors.toList());
    }

    private String buildTicketContext() {
        List<Map<String, Object>> tickets = userClient.getAllTickets();
        return "Support Tickets: " + tickets.size();
    }

    private List<Map<String, Object>> getTicketData() {
        return userClient.getAllTickets().stream().map(t -> Map.of("subject", t.get("subject"))).collect(Collectors.toList());
    }

    private String buildLowStockContext() {
        List<Map<String, Object>> low = stockClient.getAllShelves().stream()
                .filter(s -> {
                    Object c = s.get("currentQte");
                    Object m = s.get("minQte");
                    return c != null && m != null && ((Number) c).intValue() <= ((Number) m).intValue();
                }).collect(Collectors.toList());
        return "Low Stock: " + low.size() + " items.";
    }

    private List<Map<String, Object>> getLowStockData() {
        return stockClient.getAllShelves().stream().filter(s -> s.get("currentQte") != null).map(s -> Map.of("shelf", s.get("nb"), "qty", s.get("currentQte"))).collect(Collectors.toList());
    }

    private String buildRequestStatusContext(String role, String userId) {
        return "Requests: " + technicianClient.getAllPartRequests().size();
    }

    private List<Map<String, Object>> getRequestStatusData(String role, String userId) {
        return technicianClient.getAllPartRequests().stream().map(r -> Map.of("status", r.get("status"))).collect(Collectors.toList());
    }

    private String buildPartsAvailabilityContext() {
        return "Available: " + stockClient.getEquipmentByStatus("Available").size();
    }

    private List<Map<String, Object>> getPartsAvailabilityData() {
        return stockClient.getEquipmentByStatus("Available").stream().map(e -> Map.of("name", e.get("equipmentName"))).collect(Collectors.toList());
    }

    private String buildEquipmentStatusContext() {
        return "Broken/Maintenance: " + stockClient.getEquipmentByStatus("Broken").size();
    }

    private List<Map<String, Object>> getEquipmentStatusData() {
        return stockClient.getEquipmentByStatus("Broken").stream().map(e -> Map.of("name", e.get("equipmentName"))).collect(Collectors.toList());
    }

    private String detectStatus(String message) {
        String lower = message.toLowerCase();
        for (Map.Entry<String, String> entry : STATUS_MAP.entrySet()) {
            if (lower.contains(entry.getKey())) return entry.getValue();
        }
        return null;
    }
}
