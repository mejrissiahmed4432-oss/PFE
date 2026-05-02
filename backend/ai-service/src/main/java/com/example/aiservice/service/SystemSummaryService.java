package com.example.aiservice.service;

import com.example.aiservice.clients.StockClient;
import com.example.aiservice.clients.TechnicianClient;
import com.example.aiservice.clients.UserClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * SystemSummaryService — pre-computes structured aggregate stats before the LLM is called.
 *
 * This ensures the LLM receives computed facts, not raw data to recalculate.
 * The LLM's job is to REASON over these facts, not to COUNT or AGGREGATE.
 *
 * Used primarily in GENERAL UNDERSTANDING MODE and HYBRID mode.
 */
@Service
public class SystemSummaryService {

    private static final Logger log = LoggerFactory.getLogger(SystemSummaryService.class);

    private final StockClient stockClient;
    private final TechnicianClient technicianClient;
    private final UserClient userClient;

    public SystemSummaryService(StockClient stockClient,
                                TechnicianClient technicianClient,
                                UserClient userClient) {
        this.stockClient        = stockClient;
        this.technicianClient   = technicianClient;
        this.userClient         = userClient;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // PUBLIC: Build context string for the LLM prompt
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Builds a role-tailored summary context block to inject into the LLM prompt.
     * For GENERAL UNDERSTANDING MODE and ANALYTICAL questions.
     */
    public String buildSummaryContext(String role, String userId) {
        return switch (role.toLowerCase()) {
            case "stock_manager" -> buildStockManagerSummary();
            case "technician"    -> buildTechnicianSummary(userId);
            default              -> buildGenericSummary();
        };
    }

    /**
     * Returns the computed stats as a structured map for response data attachment.
     */
    public Map<String, Object> computeStats(String role, String userId) {
        return switch (role.toLowerCase()) {
            case "stock_manager" -> computeStockManagerStats();
            case "technician"    -> computeTechnicianStats(userId);
            default              -> new LinkedHashMap<>();
        };
    }

    // ─────────────────────────────────────────────────────────────────────────
    // STOCK MANAGER SUMMARY
    // ─────────────────────────────────────────────────────────────────────────

    private String buildStockManagerSummary() {
        StringBuilder sb = new StringBuilder("=== SYSTEM INTELLIGENCE SUMMARY (Stock Manager) ===\n\n");

        try {
            List<Map<String, Object>> equipment = stockClient.getAllEquipment();
            long total       = equipment.size();
            long available   = count(equipment, "status", "Available");
            long broken      = count(equipment, "status", "Broken");
            long maintenance = count(equipment, "status", "Maintenance");
            long allocated   = count(equipment, "status", "Allocated");

            sb.append("📦 EQUIPMENT OVERVIEW:\n");
            sb.append(String.format("  • Total equipment: %d\n", total));
            sb.append(String.format("  • Available: %d (%.0f%%)\n", available, pct(available, total)));
            sb.append(String.format("  • Allocated: %d (%.0f%%)\n", allocated, pct(allocated, total)));
            sb.append(String.format("  • In Maintenance: %d\n", maintenance));
            sb.append(String.format("  • Broken: %d\n", broken));
            sb.append(String.format("  • Overall health: %s\n\n", assessEquipmentHealth(broken, maintenance, total)));

        } catch (Exception e) {
            sb.append("  Equipment data unavailable.\n\n");
            log.warn("Failed to compute equipment summary: {}", e.getMessage());
        }

        try {
            List<Map<String, Object>> shelves = stockClient.getAllShelves();
            List<Map<String, Object>> lowStock = shelves.stream()
                    .filter(s -> {
                        Object c = s.get("currentQte");
                        Object m = s.get("minQte");
                        return c != null && m != null &&
                               ((Number) c).intValue() <= ((Number) m).intValue();
                    })
                    .collect(Collectors.toList());

            sb.append("🗄️ STOCK / SHELVES:\n");
            sb.append(String.format("  • Total shelf slots: %d\n", shelves.size()));
            sb.append(String.format("  • Low stock items: %d\n", lowStock.size()));
            if (!lowStock.isEmpty()) {
                sb.append("  • Critical items: ");
                sb.append(lowStock.stream()
                        .limit(5)
                        .map(s -> String.valueOf(s.getOrDefault("nb", "?")))
                        .collect(Collectors.joining(", ")));
                sb.append("\n");
            }
            sb.append("\n");

        } catch (Exception e) {
            sb.append("  Shelf data unavailable.\n\n");
        }

        try {
            List<Map<String, Object>> suppliers = stockClient.getAllSuppliers();
            long total = suppliers.size();

            OptionalDouble avgRating = suppliers.stream()
                    .map(s -> s.get("rating"))
                    .filter(Objects::nonNull)
                    .mapToDouble(r -> ((Number) r).doubleValue())
                    .average();

            Optional<Map<String, Object>> topSupplier = suppliers.stream()
                    .filter(s -> s.get("rating") != null)
                    .max(Comparator.comparingDouble(s -> ((Number) s.get("rating")).doubleValue()));

            sb.append("🤝 SUPPLIERS:\n");
            sb.append(String.format("  • Total suppliers: %d\n", total));
            avgRating.ifPresent(avg -> sb.append(String.format("  • Average rating: %.1f/5\n", avg)));
            topSupplier.ifPresent(s -> sb.append(String.format("  • Top-rated supplier: %s (%.1f stars)\n",
                    s.getOrDefault("companyName", "N/A"),
                    ((Number) s.get("rating")).doubleValue())));
            sb.append("\n");

        } catch (Exception e) {
            sb.append("  Supplier data unavailable.\n\n");
        }

        try {
            List<Map<String, Object>> categories = stockClient.getAllCategories();
            sb.append("🏷️ CATEGORIES:\n");
            sb.append(String.format("  • Total categories defined: %d\n\n", categories.size()));
        } catch (Exception e) {
            sb.append("  Category data unavailable.\n\n");
        }

        try {
            List<Map<String, Object>> requests = technicianClient.getAllPartRequests();
            long pending  = count(requests, "status", "Pending");
            long approved = count(requests, "status", "Approved");
            sb.append("📋 PART REQUESTS:\n");
            sb.append(String.format("  • Total: %d | Pending: %d | Approved: %d\n\n",
                    requests.size(), pending, approved));
        } catch (Exception e) {
            sb.append("  Request data unavailable.\n\n");
        }

        return sb.toString();
    }

    private Map<String, Object> computeStockManagerStats() {
        Map<String, Object> stats = new LinkedHashMap<>();
        try {
            List<Map<String, Object>> equipment = stockClient.getAllEquipment();
            stats.put("totalEquipment", equipment.size());
            stats.put("available",   count(equipment, "status", "Available"));
            stats.put("broken",      count(equipment, "status", "Broken"));
            stats.put("maintenance", count(equipment, "status", "Maintenance"));
            stats.put("allocated",   count(equipment, "status", "Allocated"));
        } catch (Exception ignored) {}
        try {
            stats.put("totalSuppliers", stockClient.getAllSuppliers().size());
        } catch (Exception ignored) {}
        try {
            List<Map<String, Object>> shelves = stockClient.getAllShelves();
            long low = shelves.stream().filter(s -> {
                Object c = s.get("currentQte"); Object m = s.get("minQte");
                return c != null && m != null && ((Number) c).intValue() <= ((Number) m).intValue();
            }).count();
            stats.put("lowStockCount", low);
        } catch (Exception ignored) {}
        return stats;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // TECHNICIAN SUMMARY
    // ─────────────────────────────────────────────────────────────────────────

    private String buildTechnicianSummary(String userId) {
        StringBuilder sb = new StringBuilder("=== SYSTEM INTELLIGENCE SUMMARY (Technician) ===\n\n");

        try {
            List<Map<String, Object>> allEquipment = stockClient.getAllEquipment();
            long broken      = count(allEquipment, "status", "Broken");
            long maintenance = count(allEquipment, "status", "Maintenance");
            long total       = allEquipment.size();

            sb.append("🖥️ EQUIPMENT STATUS:\n");
            sb.append(String.format("  • Total equipment in system: %d\n", total));
            sb.append(String.format("  • Needs attention (Broken): %d\n", broken));
            sb.append(String.format("  • In Maintenance: %d\n", maintenance));
            if (broken > 0) {
                sb.append("  • Equipment needing repair: ");
                sb.append(allEquipment.stream()
                        .filter(e -> "Broken".equalsIgnoreCase(String.valueOf(e.get("status"))))
                        .limit(5)
                        .map(e -> String.valueOf(e.getOrDefault("equipmentName", "?")))
                        .collect(Collectors.joining(", ")));
                sb.append("\n");
            }
            sb.append("\n");

        } catch (Exception e) {
            sb.append("  Equipment data unavailable.\n\n");
        }

        try {
            List<Map<String, Object>> allRequests = technicianClient.getAllPartRequests();

            // Filter by userId if possible
            List<Map<String, Object>> myRequests = userId == null || userId.equals("unknown")
                    ? allRequests
                    : allRequests.stream()
                        .filter(r -> userId.equals(String.valueOf(r.getOrDefault("requesterId", "")))
                                  || userId.equals(String.valueOf(r.getOrDefault("userId", ""))))
                        .collect(Collectors.toList());

            long pending  = count(myRequests, "status", "Pending");
            long approved = count(myRequests, "status", "Approved");
            long rejected = count(myRequests, "status", "Rejected");

            sb.append("📋 YOUR PART REQUESTS:\n");
            sb.append(String.format("  • Total requests: %d\n", myRequests.size()));
            sb.append(String.format("  • Pending: %d | Approved: %d | Rejected: %d\n\n",
                    pending, approved, rejected));

        } catch (Exception e) {
            sb.append("  Part request data unavailable.\n\n");
        }

        try {
            List<Map<String, Object>> tickets = userClient.getAllTickets();
            long open   = count(tickets, "status", "Open");
            long closed = count(tickets, "status", "Closed");

            sb.append("🎫 TICKETS:\n");
            sb.append(String.format("  • Total: %d | Open: %d | Closed: %d\n\n",
                    tickets.size(), open, closed));

        } catch (Exception e) {
            sb.append("  Ticket data unavailable.\n\n");
        }

        try {
            List<Map<String, Object>> tasks = userClient.getAllTasks();
            long pending = tasks.stream()
                    .filter(t -> !"done".equalsIgnoreCase(String.valueOf(t.getOrDefault("status", "")))
                              && !"completed".equalsIgnoreCase(String.valueOf(t.getOrDefault("status", ""))))
                    .count();

            sb.append("📅 SCHEDULE:\n");
            sb.append(String.format("  • Total tasks: %d | Pending/In-progress: %d\n\n",
                    tasks.size(), pending));

        } catch (Exception e) {
            sb.append("  Schedule data unavailable.\n\n");
        }

        return sb.toString();
    }

    private Map<String, Object> computeTechnicianStats(String userId) {
        Map<String, Object> stats = new LinkedHashMap<>();
        try {
            List<Map<String, Object>> equipment = stockClient.getAllEquipment();
            stats.put("brokenEquipment", count(equipment, "status", "Broken"));
            stats.put("inMaintenance",   count(equipment, "status", "Maintenance"));
        } catch (Exception ignored) {}
        try {
            List<Map<String, Object>> requests = technicianClient.getAllPartRequests();
            long myPending = requests.stream()
                    .filter(r -> "Pending".equalsIgnoreCase(String.valueOf(r.getOrDefault("status", "")))
                              && (userId == null || userId.equals(String.valueOf(r.getOrDefault("requesterId", "")))))
                    .count();
            stats.put("myPendingRequests", myPending);
        } catch (Exception ignored) {}
        return stats;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // GENERIC SUMMARY
    // ─────────────────────────────────────────────────────────────────────────

    private String buildGenericSummary() {
        return buildStockManagerSummary();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // HELPER METHODS
    // ─────────────────────────────────────────────────────────────────────────

    private long count(List<Map<String, Object>> list, String key, String value) {
        return list.stream()
                .filter(m -> value.equalsIgnoreCase(String.valueOf(m.getOrDefault(key, ""))))
                .count();
    }

    private double pct(long part, long total) {
        return total == 0 ? 0 : (part * 100.0 / total);
    }

    private String assessEquipmentHealth(long broken, long maintenance, long total) {
        if (total == 0) return "Unknown";
        double problemRate = (broken + maintenance) * 100.0 / total;
        if (problemRate == 0)   return "✅ Excellent — all equipment operational";
        if (problemRate < 10)   return "🟢 Good — minor issues present";
        if (problemRate < 25)   return "🟡 Fair — several items need attention";
        if (problemRate < 50)   return "🟠 Poor — significant portion unavailable";
        return "🔴 Critical — majority of equipment has issues";
    }
}
