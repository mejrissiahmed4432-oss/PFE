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
                RULES:
                - Always think step by step before providing your final answer.
                - Answer the USER QUESTION accurately. If the DATA CONTEXT contains relevant data, prioritize it.
                - For follow-up questions (e.g., "give me his review", "what is his rating"), resolve from conversation history.
                - NEVER invent quantitative facts that are not present in the DATA CONTEXT.
                - For general knowledge questions (comparisons, explanations, how-to, best practices), you may use your own knowledge even if DATA CONTEXT is empty.
                - If the user asks something unrelated to IT/stock (e.g., jokes, math, general chat), answer it helpfully and briefly.
                - Format your response professionally using Markdown (tables, bold text, bullet points) where helpful.
                - Keep answers concise. Limit to 600 words maximum.
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
                - Let's think step by step. Break down the user's requirements logically before giving your final recommendation.
                - Act as a trusted advisor. Answer the user's question with confidence and expertise.
                - For comparisons (e.g., "i5 vs i7"), give clear pros/cons for each option and use comparison tables.
                - For placement advice, consider physical environment, network proximity, and usage.
                - For equipment recommendations, synthesize all provided DATA CONTEXT and find the optimal fit.
                - Always conclude with a clear, actionable recommendation.
                - Format responses clearly: use Markdown headers, bold text, tables where helpful, and a final "Recommendation:" section.
                - Keep answers under 600 words.
                
                IMPORTANT: You may use your general IT knowledge in addition to the DATA CONTEXT. Provide highly intelligent and nuanced insights.
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
            default -> "If the DATA CONTEXT has relevant information, use it. Otherwise answer from your general IT knowledge and be helpful.";
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
            default -> "If the DATA CONTEXT has relevant information, use it. Otherwise, use your technical knowledge to help the technician.";
        };

        return roleContext + "\n" + intentGuidance + "\n" + guardrails;
    }

    // ── Generic ───────────────────────────────────────────────────────────────

    private String buildGenericPrompt(String guardrails) {
        return """
                You are a helpful AI Assistant for an IT Asset Management platform.
                Answer questions using the DATA CONTEXT when relevant, or use your general knowledge for other topics.
                Be friendly, concise and professional.
                """ + "\n" + guardrails;
    }
}
