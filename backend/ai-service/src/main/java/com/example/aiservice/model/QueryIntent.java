package com.example.aiservice.model;

import java.util.Set;

/**
 * All supported AI intents across all roles.
 * Each intent is mapped to one or more roles that can use it.
 */
public enum QueryIntent {

    // ─── Stock Manager Intents ───────────────────────────────────────────────
    STOCK_STATUS("stock_manager"),
    LOW_STOCK("stock_manager"),
    RECOMMENDATION("stock_manager"),
    USAGE_ANALYSIS("stock_manager"),
    SUPPLIER_INFO("stock_manager"),
    CATEGORY_INFO("stock_manager"),

    // ─── Technician Intents ──────────────────────────────────────────────────
    EQUIPMENT_STATUS("technician"),
    PARTS_AVAILABILITY("technician"),
    MAINTENANCE_HELP("technician"),
    TICKET_STATUS("technician"),

    // ─── Shared Intents (all roles) ──────────────────────────────────────────
    REQUEST_STATUS("all"),
    SCHEDULE_INFO("all"),
    TICKET_INFO("all"),
    EQUIPMENT_SUGGESTION("all"),
    HELP("all"),
    GENERAL_ASSISTANCE("all");

    private final String role;

    QueryIntent(String role) {
        this.role = role;
    }

    public String getRole() {
        return role;
    }

    /** Returns true if this intent is accessible by the given role */
    public boolean isAccessibleBy(String userRole) {
        if ("all".equals(this.role)) return true;
        if ("admin".equalsIgnoreCase(userRole)) return true; // Admins can access everything
        return this.role.equalsIgnoreCase(userRole);
    }

    /** All intent names accessible by a given role */
    public static Set<QueryIntent> forRole(String role) {
        Set<QueryIntent> result = new java.util.LinkedHashSet<>();
        for (QueryIntent intent : values()) {
            if (intent.isAccessibleBy(role)) {
                result.add(intent);
            }
        }
        return result;
    }
}
