package com.example.aiservice.service;

import com.example.aiservice.model.QueryIntent;
import org.springframework.stereotype.Service;

/**
 * Builds the final augmented prompt sent to the LLM.
 * Each role and intent gets a tailored system prompt with strict guardrails.
 */
@Service
public class PromptBuilderService {

    public String build(QueryIntent intent, String role, String context, String question) {
        String systemInstructions = getSystemInstructions(role, intent);
        return systemInstructions + "\n\n" +
               "=== DATA CONTEXT ===\n" + context + "\n\n" +
               "=== USER QUESTION ===\n" + question;
    }

    private String getSystemInstructions(String role, QueryIntent intent) {
        // Special: EQUIPMENT_SUGGESTION uses expert knowledge, not strict data-only mode
        if (intent == QueryIntent.EQUIPMENT_SUGGESTION) {
            return buildSuggestionPrompt(role);
        }

        String baseGuardrails = """
                CRITICAL RULES (NEVER VIOLATE):
                - Answer the USER QUESTION accurately using the DATA CONTEXT and the conversation history.
                - If the question is a follow-up (e.g., "give me his review", "what is his rating"), resolve it
                  from the previous context in this conversation.
                - Use ONLY the data provided in the DATA CONTEXT section.
                - If the DATA CONTEXT includes a 'FILTERED LIST', prioritize that information.
                - NEVER invent, assume, or hallucinate any information.
                - If the data is truly insufficient, respond: "No sufficient data available."
                - Be concise, practical, and professional. Use bullet points for lists.
                - Limit your answer to 300 words maximum.
                """;

        return switch (role.toLowerCase()) {
            case "stock_manager" -> buildStockManagerPrompt(intent, baseGuardrails);
            case "technician"    -> buildTechnicianPrompt(intent, baseGuardrails);
            default              -> buildGenericPrompt(baseGuardrails);
        };
    }

    // ── Equipment Suggestion (expert mode, not data-only) ─────────────────────

    private String buildSuggestionPrompt(String role) {
        return """
                You are an expert IT Asset Management Consultant with deep knowledge of hardware,
                software, and IT infrastructure.
                
                YOUR TASK:
                - Act as a trusted advisor. Answer the user's question with confidence and expertise.
                - For comparisons (e.g., "i5 vs i7"), give clear pros/cons for each option.
                - For placement advice, consider physical environment, network proximity, and usage.
                - For equipment recommendations, consider the user's context from the DATA CONTEXT.
                - Always conclude with a clear, actionable recommendation.
                - Format responses clearly: use headers, bullet points, and a final "Recommendation:" section.
                - Keep answers under 400 words.
                
                IMPORTANT: You may use your general IT knowledge in addition to the DATA CONTEXT.
                """;
    }

    // ── Stock Manager ─────────────────────────────────────────────────────────

    private String buildStockManagerPrompt(QueryIntent intent, String guardrails) {
        String roleContext = """
                You are a highly intelligent IT Asset Management AI Assistant for a Stock Manager.
                You have real-time access to equipment, shelves, suppliers, categories, and requests.
                
                YOUR ROLE:
                1. ANALYZE stock health and identify issues.
                2. ANSWER follow-up questions using conversation history.
                3. REPORT exact data (ratings, contact details, category names).
                4. ADVISE on restocking priorities and supplier selection.
                """;

        String intentGuidance = switch (intent) {
            case STOCK_STATUS -> """
                    Report: total equipment count, breakdown by status (Available/Broken/Maintenance).
                    Highlight any critical situations. Summarize overall stock health.
                    """;
            case LOW_STOCK -> """
                    List items below minimum threshold. Format: [URGENT/WARNING] Item — Stock: X (min: Y).
                    Recommend immediate reorder actions.
                    """;
            case RECOMMENDATION -> """
                    Identify what should be reordered. Be specific: name, quantity, reason.
                    Prioritize by urgency and current stock levels.
                    """;
            case USAGE_ANALYSIS -> """
                    Identify most used equipment types. Spot consumption trends.
                    Suggest stock adjustments accordingly.
                    """;
            case SUPPLIER_INFO -> """
                    Report: company name, rating (stars), review/note, contact person, email, category.
                    When asked for a specific detail (rating, review, contact), answer directly.
                    """;
            case CATEGORY_INFO -> """
                    List all equipment categories and their sub-types.
                    Report counts and icons as defined in the system.
                    """;
            default -> "Provide a data-driven response based on the context.";
        };

        return roleContext + "\n" + intentGuidance + "\n" + guardrails;
    }

    // ── Technician ────────────────────────────────────────────────────────────

    private String buildTechnicianPrompt(QueryIntent intent, String guardrails) {
        String roleContext = """
                You are an expert Technical Support AI Assistant for a Technician.
                You help with equipment status, spare parts, maintenance guidance, and requests.
                YOUR ROLE: Be precise, technical, and provide clear step-by-step guidance.
                """;

        String intentGuidance = switch (intent) {
            case EQUIPMENT_STATUS -> """
                    Report the current condition of the equipment: status, specs, assigned location.
                    Provide technical details relevant to the task.
                    """;
            case PARTS_AVAILABILITY -> """
                    State clearly which parts are available (quantity, shelf, location).
                    If unavailable, suggest submitting a part request.
                    """;
            case MAINTENANCE_HELP -> """
                    Provide step-by-step maintenance or repair guidance.
                    Reference equipment specs from the context when available.
                    """;
            case TICKET_STATUS -> """
                    Summarize open maintenance tickets with status and priorities.
                    """;
            case SUPPLIER_INFO -> """
                    Report supplier details relevant to spare parts procurement.
                    Include ratings and contact information.
                    """;
            default -> "Provide technical guidance based on the equipment and maintenance data.";
        };

        return roleContext + "\n" + intentGuidance + "\n" + guardrails;
    }

    // ── Generic ───────────────────────────────────────────────────────────────

    private String buildGenericPrompt(String guardrails) {
        return """
                You are a helpful IT Asset Management AI Assistant.
                Answer using the DATA CONTEXT provided and conversation history for follow-up resolution.
                """ + "\n" + guardrails;
    }
}
