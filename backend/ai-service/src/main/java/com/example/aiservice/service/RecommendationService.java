package com.example.aiservice.service;

import com.example.aiservice.model.QueryIntent;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Extracts actionable suggestions from the AI answer and intent context.
 * Suggestions are specific, role-appropriate, and data-driven.
 */
@Service
public class RecommendationService {

    /**
     * Generate suggestions based on intent, role, the AI answer, and structured data.
     */
    public List<String> generate(QueryIntent intent, String role,
                                  String aiAnswer, List<Map<String, Object>> data) {
        List<String> suggestions = new ArrayList<>();

        // Add intent-specific rule-based suggestions from structured data
        suggestions.addAll(ruleBasedSuggestions(intent, role, data));

        // Add suggestions extracted from the AI answer text
        suggestions.addAll(extractFromAnswer(aiAnswer));

        // Deduplicate and cap
        return suggestions.stream()
                .distinct()
                .filter(s -> s != null && !s.isBlank())
                .limit(5)
                .toList();
    }

    // ── Rule-based suggestions from structured data ───────────────────────────

    private List<String> ruleBasedSuggestions(QueryIntent intent, String role,
                                               List<Map<String, Object>> data) {
        List<String> suggestions = new ArrayList<>();

        switch (intent) {
            case LOW_STOCK -> {
                for (Map<String, Object> shelf : data) {
                    String type = String.valueOf(shelf.getOrDefault("type", "Unknown"));
                    int current = ((Number) shelf.getOrDefault("current", 0)).intValue();

                    if (current == 0) {
                        suggestions.add("🔴 URGENT: Reorder " + type + " immediately — stock is empty.");
                    } else {
                        suggestions.add("⚠️ Create a purchase request for " + type +
                                        " (current: " + current + " units).");
                    }
                }
                if (!data.isEmpty()) {
                    suggestions.add("📋 Review reorder thresholds for understocked item types.");
                }
            }

            case STOCK_STATUS -> {
                long broken = data.stream()
                        .filter(e -> "Broken".equals(e.get("status")))
                        .count();
                long maintenance = data.stream()
                        .filter(e -> "Maintenance".equals(e.get("status")))
                        .count();

                if (broken > 0)
                    suggestions.add("🔧 Review " + broken + " broken item(s) — consider repair or disposal.");
                if (maintenance > 0)
                    suggestions.add("📌 Follow up on " + maintenance + " item(s) currently in maintenance.");
                if (data.size() > 20)
                    suggestions.add("📊 Run a quarterly stock audit to remove obsolete assets.");
            }

            case RECOMMENDATION -> {
                suggestions.add("📦 Create purchase requests for recommended items through the Requests module.");
                suggestions.add("🔁 Set up automatic reorder alerts for frequently depleted stock.");
                suggestions.add("🤝 Contact your preferred supplier for bulk order discounts.");
            }

            case USAGE_ANALYSIS -> {
                suggestions.add("📈 Increase stock levels for high-demand item types.");
                suggestions.add("📉 Consider reducing stock for items with no usage in 90+ days.");
                suggestions.add("🏷️ Tag high-frequency items for priority shelf placement.");
            }

            case PARTS_AVAILABILITY -> {
                if (data.isEmpty()) {
                    suggestions.add("📬 Submit a part request to the stock manager for the needed components.");
                    suggestions.add("⏳ Check back after your request is approved.");
                } else {
                    suggestions.add("✅ Visit the stock room to retrieve available parts.");
                    suggestions.add("📝 Log part usage in the equipment maintenance record.");
                }
            }

            case EQUIPMENT_STATUS -> {
                long brokenCount = data.stream()
                        .filter(e -> "Broken".equals(e.get("status")))
                        .count();
                if (brokenCount > 0)
                    suggestions.add("🔴 Prioritize repair or replacement for " + brokenCount + " broken device(s).");
                suggestions.add("📋 Update the maintenance ticket after completing repairs.");
                suggestions.add("🔑 Ensure repaired equipment is returned with updated status.");
            }

            case REQUEST_STATUS -> {
                boolean hasPending = data.stream()
                        .anyMatch(r -> "Pending".equalsIgnoreCase(String.valueOf(r.getOrDefault("status", ""))));
                if ("stock_manager".equalsIgnoreCase(role) && hasPending)
                    suggestions.add("⚡ Review and process pending part requests from technicians.");
                if ("technician".equalsIgnoreCase(role))
                    suggestions.add("📞 Contact the stock manager if your request has been pending too long.");
            }

            default -> {}
        }

        return suggestions;
    }

    // ── Extract suggestions from LLM answer text ─────────────────────────────

    private List<String> extractFromAnswer(String answer) {
        if (answer == null || answer.isBlank()) return Collections.emptyList();

        List<String> extracted = new ArrayList<>();

        // Match bullet-point lines starting with -, •, *, or numbered
        Pattern bulletPattern = Pattern.compile("^[\\s]*[-•*✅⚠️🔴📦📋🔧📊]+\\s*(.{10,120})$",
                Pattern.MULTILINE);
        Matcher matcher = bulletPattern.matcher(answer);

        while (matcher.find() && extracted.size() < 3) {
            String candidate = matcher.group(1).trim();
            if (candidate.length() > 10 && !candidate.endsWith(":")) {
                extracted.add(candidate);
            }
        }

        return extracted;
    }
}
