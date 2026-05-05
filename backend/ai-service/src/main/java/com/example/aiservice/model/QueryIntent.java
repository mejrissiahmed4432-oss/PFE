package com.example.aiservice.model;

import java.util.Set;

/**
 * All supported AI intents across all roles.
 * Each intent is mapped to one or more roles that can use it.
 */
public enum QueryIntent {

<<<<<<< HEAD
    // ─── Stock Manager Intents ───────────────────────────────────────
=======
    // ─── Stock Manager Intents ───────────────────────────────────────────────
>>>>>>> my-local-work
    STOCK_STATUS("stock_manager"),
    LOW_STOCK("stock_manager"),
    RECOMMENDATION("stock_manager"),
    USAGE_ANALYSIS("stock_manager"),
    SUPPLIER_INFO("stock_manager"),
    CATEGORY_INFO("stock_manager"),

<<<<<<< HEAD
    // ─── Stock Manager Actions ───────────────────────────────────────
    ADD_EQUIPMENT("stock_manager"),
    UPDATE_EQUIPMENT("stock_manager"),
    APPROVE_REQUEST("stock_manager"),
    REJECT_REQUEST("stock_manager"),

    // ─── Technician Intents ────────────────────────────────────────
=======
    // ─── Technician Intents ──────────────────────────────────────────────────
>>>>>>> my-local-work
    EQUIPMENT_STATUS("technician"),
    PARTS_AVAILABILITY("technician"),
    MAINTENANCE_HELP("technician"),
    TICKET_STATUS("technician"),

<<<<<<< HEAD
    // ─── Technician Actions ────────────────────────────────────────
    SUBMIT_PART_REQUEST("technician"),

    // ─── Shared Intents (all roles) ────────────────────────────────────
=======
    // ─── Shared Intents (all roles) ──────────────────────────────────────────
>>>>>>> my-local-work
    REQUEST_STATUS("all"),
    SCHEDULE_INFO("all"),
    TICKET_INFO("all"),
    EQUIPMENT_SUGGESTION("all"),
    HELP("all"),
<<<<<<< HEAD
    GENERAL_ASSISTANCE("all"),

    // ─── Shared Actions (all roles) ───────────────────────────────────
    CREATE_TASK("all"),
    UPDATE_TICKET("all");
=======
    GENERAL_ASSISTANCE("all");
>>>>>>> my-local-work

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
