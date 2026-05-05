package com.example.aiservice.service;

import com.example.aiservice.model.QueryIntent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * QueryUnderstandingService — the "brain" of the contextual assistant pipeline.
 *
 * Responsibilities:
 * 1. Resolve pronouns/references using conversation history (he, it, that supplier…)
 * 2. Extract named entities (supplier names, equipment models, part types)
 * 3. Classify question type: FACTUAL | ANALYTICAL | COMPARISON | RECOMMENDATION | OPEN_ENDED
 * 4. Determine execution path: DIRECT | RAG | HYBRID | GENERAL
 * 5. Produce a QueryUnderstanding that drives all downstream services
 */
@Service
public class QueryUnderstandingService {

    private static final Logger log = LoggerFactory.getLogger(QueryUnderstandingService.class);

    // ── Confidence threshold ──────────────────────────────────────────────────
    private static final double CONFIDENCE_THRESHOLD = 0.5;

    // ── Pronoun / reference triggers ─────────────────────────────────────────
    private static final List<String> PRONOUN_PATTERNS = List.of(
            "\\bhis\\b", "\\bher\\b", "\\bits\\b", "\\btheir\\b",
            "\\bhe\\b", "\\bshe\\b", "\\bit\\b", "\\bthey\\b",
            "\\bthat\\b", "\\bthis one\\b", "\\bthe same\\b",
            "\\bthat supplier\\b", "\\bthat equipment\\b", "\\bthat part\\b",
            "\\bcelui-là\\b", "\\bcette\\b", "\\bce fournisseur\\b",
            "\\bsame\\b", "\\babove\\b", "\\bprevious\\b"
    );

    // ── Comparison / recommendation / analytical triggers ─────────────────────
    private static final List<String> COMPARISON_KEYWORDS = List.of(
            "vs", "versus", "compare", "comparison", "better", "worse",
            "difference between", "which is", "which one", "i5 or i7",
            "meilleur", "comparer", "lequel", "différence"
    );

    private static final List<String> RECOMMENDATION_KEYWORDS = List.of(
            "recommend", "suggest", "should i", "what should", "best",
            "which to choose", "advise", "opinion", "conseiller", "conseil",
            "what do you think", "is it worth", "should we"
    );

    private static final List<String> ANALYTICAL_KEYWORDS = List.of(
            "health", "healthy", "performance", "trend", "analysis", "overview",
            "how is", "how are", "situation", "état", "santé", "vue d'ensemble",
            "overall", "summary", "what about", "tell me about",
            "is my", "are my", "how well"
    );

    private static final List<String> OPEN_ENDED_KEYWORDS = List.of(
            "what do you think", "any issues", "anything wrong", "thoughts",
            "insight", "opinion", "how would you describe", "generally",
            "in general", "quoi de neuf", "comment va", "tout va bien"
    );

    // ── Known entity patterns ─────────────────────────────────────────────────
    private static final List<String> EQUIPMENT_PATTERNS = List.of(
            "laptop", "pc", "computer", "server", "router", "switch", "monitor",
            "keyboard", "mouse", "ram", "ssd", "hdd", "cpu", "processor", "gpu",
            "dell", "hp", "asus", "lenovo", "apple", "samsung", "acer",
            "latitude", "elitebook", "vivobook", "thinkpad", "macbook"
    );

    private static final List<String> PART_PATTERNS = List.of(
            "ram", "ssd", "hdd", "battery", "charger", "cable", "power supply",
            "fan", "screen", "keyboard", "motherboard", "processor", "gpu", "memory"
    );

    // ── Public API ────────────────────────────────────────────────────────────

    /**
     * Main entry point: analyze message + history and produce a QueryUnderstanding.
     *
     * @param message    current user message
     * @param history    previous conversation turns (role → content maps)
     * @param role       user role (stock_manager | technician | admin)
     * @param intent     detected intent from IntentService
     * @param confidence intent confidence score (0.0 – 1.0)
     * @return           a fully enriched QueryUnderstanding
     */
    public QueryUnderstanding analyze(
            String message,
            List<Map<String, String>> history,
            String role,
            QueryIntent intent,
            double confidence
    ) {
        String lower = message.toLowerCase().trim();

        // 1. Resolve pronouns — rewrite message if references detected
        String resolvedMessage = resolvePronounReferences(message, lower, history);
        boolean wasResolved = !resolvedMessage.equals(message);
        if (wasResolved) {
            log.debug("Pronoun resolved: '{}' → '{}'", message, resolvedMessage);
        }

        // 2. Extract entity from message or history
        String entity = extractEntity(lower, history);

        // 3. Classify question type
        String questionType = classifyQuestionType(lower);

        // 4. Determine execution path
        String executionPath = determineExecutionPath(intent, confidence, questionType, role);

        // 5. If confidence below threshold — escalate to GENERAL_ASSISTANCE
        QueryIntent finalIntent = intent;
        if (confidence < CONFIDENCE_THRESHOLD && executionPath.equals("GENERAL")) {
            finalIntent = QueryIntent.GENERAL_ASSISTANCE;
            log.debug("Low confidence ({}) → escalated to GENERAL_ASSISTANCE", confidence);
        }

        return new QueryUnderstanding(
                resolvedMessage,
                questionType,
                executionPath,
                entity,
                confidence,
                finalIntent
        );
    }

    // ── Pronoun Resolution ────────────────────────────────────────────────────

    private String resolvePronounReferences(String original, String lower, List<Map<String, String>> history) {
        if (history == null || history.isEmpty()) return original;

        boolean hasPronoun = PRONOUN_PATTERNS.stream()
                .anyMatch(p -> Pattern.compile(p, Pattern.CASE_INSENSITIVE).matcher(lower).find());

        if (!hasPronoun) return original;

        // Find last entity mentioned in history (by assistant or user)
        String lastEntity = extractLastEntityFromHistory(history);
        if (lastEntity == null) return original;

        // Replace pronoun references with the entity
        String resolved = original;
        for (String pronoun : List.of("his", "her", "its", "their", "he", "she", "it", "they",
                "that", "this one", "the same", "that supplier", "that equipment", "that part")) {
            resolved = resolved.replaceAll("(?i)\\b" + Pattern.quote(pronoun) + "\\b", lastEntity);
        }

        return resolved.trim();
    }

    private String extractLastEntityFromHistory(List<Map<String, String>> history) {
        // Walk history in reverse to find the last mentioned entity
        for (int i = history.size() - 1; i >= 0; i--) {
            String content = history.get(i).getOrDefault("content", "").toLowerCase();
            String entity = extractEntityFromText(content);
            if (entity != null) return entity;
        }
        return null;
    }

    // ── Entity Extraction ─────────────────────────────────────────────────────

    private String extractEntity(String lower, List<Map<String, String>> history) {
        // First: look in current message
        String fromMessage = extractEntityFromText(lower);
        if (fromMessage != null) return fromMessage;

        // Fallback: look in recent history
        if (history != null) {
            return extractLastEntityFromHistory(history);
        }
        return null;
    }

    private String extractEntityFromText(String text) {
        // Check for equipment/part keywords
        for (String kw : EQUIPMENT_PATTERNS) {
            if (text.contains(kw.toLowerCase())) return capitalize(kw);
        }
        for (String kw : PART_PATTERNS) {
            if (text.contains(kw.toLowerCase())) return capitalize(kw);
        }
        // Try to extract a proper-noun-like word (capitalized word after "supplier", "company", etc.)
        Pattern properNoun = Pattern.compile(
                "(?:supplier|company|vendor|equipment|device|part|component|brand)[:\\s]+([A-Z][a-zA-Z0-9\\s]{2,30})",
                Pattern.CASE_INSENSITIVE
        );
        Matcher m = properNoun.matcher(text);
        if (m.find()) return m.group(1).trim();

        return null;
    }

    private String capitalize(String s) {
        if (s == null || s.isEmpty()) return s;
        return Character.toUpperCase(s.charAt(0)) + s.substring(1).toLowerCase();
    }

    // ── Question Type Classification ──────────────────────────────────────────

    private String classifyQuestionType(String lower) {
        if (COMPARISON_KEYWORDS.stream().anyMatch(lower::contains))   return "COMPARISON";
        if (RECOMMENDATION_KEYWORDS.stream().anyMatch(lower::contains)) return "RECOMMENDATION";
        if (OPEN_ENDED_KEYWORDS.stream().anyMatch(lower::contains))   return "OPEN_ENDED";
        if (ANALYTICAL_KEYWORDS.stream().anyMatch(lower::contains))   return "ANALYTICAL";
        return "FACTUAL";
    }

    // ── Execution Path Determination ──────────────────────────────────────────

    private String determineExecutionPath(QueryIntent intent, double confidence,
                                          String questionType, String role) {
        // RAG intents always go through vector search
        if (intent == QueryIntent.EQUIPMENT_SUGGESTION ||
            intent == QueryIntent.MAINTENANCE_HELP ||
            "COMPARISON".equals(questionType)) {
            return "RAG";
        }

        // Hybrid: analytical questions that also have a known direct intent
        if ("ANALYTICAL".equals(questionType) && confidence >= CONFIDENCE_THRESHOLD) {
            return "HYBRID";
        }

        // Open-ended or low confidence → general understanding
        if ("OPEN_ENDED".equals(questionType) || "RECOMMENDATION".equals(questionType)
                || confidence < CONFIDENCE_THRESHOLD) {
            return "GENERAL";
        }

        // High-confidence known intents → direct query
        if (confidence >= CONFIDENCE_THRESHOLD) return "DIRECT";

        return "GENERAL";
    }

    // ── Inner class: QueryUnderstanding ──────────────────────────────────────

    public static class QueryUnderstanding {

        private final String resolvedMessage;
        private final String questionType;   // FACTUAL | ANALYTICAL | COMPARISON | RECOMMENDATION | OPEN_ENDED
        private final String executionPath;  // DIRECT | RAG | HYBRID | GENERAL
        private final String detectedEntity; // e.g., "TechStore", "RAM", "Dell Latitude"
        private final double intentConfidence;
        private final QueryIntent intent;

        public QueryUnderstanding(String resolvedMessage, String questionType,
                                  String executionPath, String detectedEntity,
                                  double intentConfidence, QueryIntent intent) {
            this.resolvedMessage  = resolvedMessage;
            this.questionType     = questionType;
            this.executionPath    = executionPath;
            this.detectedEntity   = detectedEntity;
            this.intentConfidence = intentConfidence;
            this.intent           = intent;
        }

        public String getResolvedMessage()  { return resolvedMessage; }
        public String getQuestionType()     { return questionType; }
        public String getExecutionPath()    { return executionPath; }
        public String getDetectedEntity()   { return detectedEntity; }
        public double getIntentConfidence() { return intentConfidence; }
        public QueryIntent getIntent()      { return intent; }

        @Override
        public String toString() {
            return String.format("QueryUnderstanding{intent=%s, confidence=%.2f, path=%s, type=%s, entity=%s}",
                    intent, intentConfidence, executionPath, questionType, detectedEntity);
        }
    }
}
