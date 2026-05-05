package com.example.aiservice.service;

import com.example.aiservice.model.QueryIntent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Hybrid intent detection:
 * 1. Fast keyword matching (instant, no LLM call)
 * 2. LLM classification fallback (for ambiguous queries)
 */
@Service
public class IntentService {

    private static final Logger log = LoggerFactory.getLogger(IntentService.class);

    private final ChatModel chatModel;

    // ── Keyword maps per role ─────────────────────────────────────────────────

    private static final Map<QueryIntent, List<String>> STOCK_KEYWORDS = new LinkedHashMap<>();
    private static final Map<QueryIntent, List<String>> TECHNICIAN_KEYWORDS = new LinkedHashMap<>();
    private static final Map<QueryIntent, List<String>> SHARED_KEYWORDS = new LinkedHashMap<>();

    static {
        
        // Stock Manager
        STOCK_KEYWORDS.put(QueryIntent.LOW_STOCK, List.of(
                "low", "running out", "shortage", "critical", "below threshold",
                "manque", "peu", "faible", "insuffisant", "minimum"
        ));
        STOCK_KEYWORDS.put(QueryIntent.STOCK_STATUS, List.of(
                "stock", "inventory", "status", "situation", "overview", "summary",
                "total equipment", "how many equipment", "how much equipment", "stock count",
                "level", "état", "inventaire", "disponible",
                "shelf", "shelves", "étagère", "rayon"
        ));
        STOCK_KEYWORDS.put(QueryIntent.RECOMMENDATION, List.of(
                "recommend", "reorder", "order", "buy", "purchase", "should i", "suggest",
                "what to buy", "what should", "commander", "acheter", "réapprovisionner"
        ));
        STOCK_KEYWORDS.put(QueryIntent.USAGE_ANALYSIS, List.of(
                "usage", "used", "most used", "frequently", "popular", "demand",
                "consumption", "utilization", "utilisation", "consommation", "fréquent"
        ));
        STOCK_KEYWORDS.put(QueryIntent.SUPPLIER_INFO, List.of(
                "supplier", "vendor", "provider", "fournisseur", "contact", "brand"
        ));
        STOCK_KEYWORDS.put(QueryIntent.CATEGORY_INFO, List.of(
                "category", "categories", "type", "group", "class", "catégorie"
        ));

        // Technician
        TECHNICIAN_KEYWORDS.put(QueryIntent.EQUIPMENT_STATUS, List.of(
                "equipment", "device", "laptop", "pc", "computer", "machine",
                "status", "condition", "broken", "working", "appareil", "équipement"
        ));
        TECHNICIAN_KEYWORDS.put(QueryIntent.PARTS_AVAILABILITY, List.of(
                "part", "component", "available", "in stock", "spare", "replacement",
                "pièce", "composant", "disponible", "remplacement"
        ));
        TECHNICIAN_KEYWORDS.put(QueryIntent.MAINTENANCE_HELP, List.of(
                "how to", "fix", "repair", "install", "configure", "guide", "step",
                "troubleshoot", "problem", "issue", "réparer", "configurer", "problème"
        ));
        TECHNICIAN_KEYWORDS.put(QueryIntent.TICKET_STATUS, List.of(
                "ticket", "task", "assigned", "maintenance", "work order", "job",
                "tâche", "assigné", "intervention"
        ));

        // Shared
        SHARED_KEYWORDS.put(QueryIntent.REQUEST_STATUS, List.of(
                "request", "pending", "approved", "rejected", "my request", "demande",
                "en attente", "approuvé", "refusé"
        ));
        SHARED_KEYWORDS.put(QueryIntent.SCHEDULE_INFO, List.of(
                "schedule", "task", "todo", "calendar", "scheduler", "plan", "planning",
                "tâche", "calendrier", "programme"
        ));
        SHARED_KEYWORDS.put(QueryIntent.TICKET_INFO, List.of(
                "ticket", "issue", "support", "incident", "maintenance ticket",
                "ticket de support", "problème"
        ));
        SHARED_KEYWORDS.put(QueryIntent.EQUIPMENT_SUGGESTION, List.of(
                "better", "vs", "versus", "compare", "comparison", "which is", "recommend me",
                "should i choose", "best", "difference between", "i5 or i7", "i5 vs i7",
                "which processor", "placement", "where to place", "where should i put",
                "advice", "suggest", "opinion", "what do you think", "which one",
                "meilleur", "comparer", "lequel", "conseil", "avis", "placer"
        ));
        SHARED_KEYWORDS.put(QueryIntent.HELP, List.of(
                "help", "how to", "what is", "explain", "guide", "aide", "comment",
                "qu'est-ce", "expliquer"
        ));
    }

    public IntentService(ChatModel chatModel) {
        this.chatModel = chatModel;
    }

    /**
     * Detects the intent for the given message and role.
     * Uses keyword matching first, LLM as fallback.
     */
    public QueryIntent detect(String message, String role) {
        String normalized = message.toLowerCase().trim();

        // 1. Try keyword-based detection
        QueryIntent keywordIntent = keywordMatch(normalized, role);
        if (keywordIntent != null) {
            log.debug("Intent detected by keywords: {} for role: {}", keywordIntent, role);
            return keywordIntent;
        }

        // 2. Fallback: LLM classification
        log.debug("Keyword match failed — using LLM classification for role: {}", role);
        return llmClassify(message, role);
    }

    // ── Keyword matching ──────────────────────────────────────────────────────

    private QueryIntent keywordMatch(String normalized, String role) {
        Map<QueryIntent, List<String>> roleMap = getRoleKeywords(role);

        // Score each intent
        QueryIntent best = null;
        int bestScore = 0;

        // Combine role-specific + shared keywords
        Map<QueryIntent, List<String>> combined = new LinkedHashMap<>(roleMap);
        combined.putAll(SHARED_KEYWORDS);

        for (Map.Entry<QueryIntent, List<String>> entry : combined.entrySet()) {
            if (!entry.getKey().isAccessibleBy(role)) continue;
            int score = 0;
            for (String keyword : entry.getValue()) {
                if (normalized.contains(keyword)) score++;
            }
            if (score > bestScore) {
                bestScore = score;
                best = entry.getKey();
            }
        }

        return (bestScore > 0) ? best : null;
    }

    private Map<QueryIntent, List<String>> getRoleKeywords(String role) {
        return switch (role.toLowerCase()) {
            case "stock_manager" -> STOCK_KEYWORDS;
            case "technician" -> TECHNICIAN_KEYWORDS;
            default -> SHARED_KEYWORDS;
        };
    }

    // ── LLM classification ────────────────────────────────────────────────────

    private QueryIntent llmClassify(String message, String role) {
        Set<QueryIntent> allowedIntents = QueryIntent.forRole(role);
        String intentList = allowedIntents.stream()
                .map(Enum::name)
                .reduce((a, b) -> a + ", " + b)
                .orElse("GENERAL_ASSISTANCE");

        String systemPrompt = """
                You are an intent classifier. Classify the user message into EXACTLY ONE intent.
                Available intents for role '%s': [%s]
                Rules:
                - Return ONLY the intent name in UPPERCASE, nothing else.
                - If unsure, return GENERAL_ASSISTANCE.
                - Do NOT add punctuation, explanations, or extra text.
                """.formatted(role, intentList);

        try {
            String response = chatModel.call(
                    new Prompt(List.of(
                            new SystemMessage(systemPrompt),
                            new UserMessage(message)
                    ))
            ).getResult().getOutput().getText().trim().toUpperCase();

            // Extract intent name from response (in case LLM adds extra text)
            Pattern pattern = Pattern.compile(
                    Arrays.stream(QueryIntent.values())
                          .map(Enum::name)
                          .reduce((a, b) -> a + "|" + b).orElse("GENERAL_ASSISTANCE")
            );
            Matcher matcher = pattern.matcher(response);
            if (matcher.find()) {
                QueryIntent detected = QueryIntent.valueOf(matcher.group());
                if (detected.isAccessibleBy(role)) {
                    log.debug("LLM classified intent: {}", detected);
                    return detected;
                }
            }
        } catch (Exception e) {
            log.warn("LLM intent classification failed: {}", e.getMessage());
        }

        return QueryIntent.GENERAL_ASSISTANCE;
    }
}
