package com.example.aiservice.service;

import com.example.aiservice.model.AiResponse;
import com.example.aiservice.model.QueryIntent;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * Uses LLM to detect if a message is an actionable command (create, update, delete).
 * Extracts structured parameters from natural language.
 */
@Service
public class ActionDetectorService {

    private static final Logger log = LoggerFactory.getLogger(ActionDetectorService.class);
    private final ChatModel chatModel;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public ActionDetectorService(ChatModel chatModel) {
        this.chatModel = chatModel;
    }

    /**
     * Analyzes the message to see if it triggers a write action.
     * Returns true if an action is detected and updates the AiResponse with payload.
     */
    public boolean detectAndPopulate(String message, String role, String userId, AiResponse response) {
        // Only trigger for specific keywords to save on API calls
        String lower = message.toLowerCase();
        boolean potentialAction = lower.contains("add") || lower.contains("create") || 
                                 lower.contains("update") || lower.contains("approve") || 
                                 lower.contains("reject") || lower.contains("submit") ||
                                 lower.contains("delete") || lower.contains("remove") ||
                                 lower.contains("send") || lower.contains("message") ||
                                 lower.contains("notify") || lower.contains("tell");

        if (!potentialAction) return false;

        String systemPrompt = """
                You are an Intent Extraction Engine for an IT Asset Management platform.
                OUTPUT: A single raw JSON object. No markdown. No explanation. No code fences.

                ═══════════════════════════════════════
                CURRENT USER
                  Role    : %s
                  User ID : %s
                ═══════════════════════════════════════

                ════════════════════════════════════════════════════════
                PERMISSION MATRIX — return {"actionDetected":false} if role is not in the ✓ column
                ────────────────────────────────────────────────────────
                Action                stock_manager  it_manager  technician  admin
                ADD_EQUIPMENT              ✓             ✓            ✗         ✓
                UPDATE_EQUIPMENT           ✓             ✓            ✗         ✓
                DELETE_EQUIPMENT           ✓             ✓            ✗         ✓
                APPROVE_REQUEST            ✓             ✓            ✗         ✓
                REJECT_REQUEST             ✓             ✓            ✗         ✓
                SUBMIT_PART_REQUEST        ✗             ✗            ✓         ✓
                CREATE_TASK                ✓             ✓            ✓         ✓
                UPDATE_TICKET              ✓             ✓            ✓         ✓
                SEND_MESSAGE               ✓             ✓            ✓         ✓
                ════════════════════════════════════════════════════════

                ════════════════════════════════════════════════════════
                CORE PHILOSOPHY — READ THIS FIRST
                ────────────────────────────────────────────────────────
                NEVER refuse to perform an action because a non-critical field is missing.
                ALWAYS use intelligent defaults and proceed.
                ONLY block (return actionDetected:false or ask) if a CRITICAL field with NO
                possible default or inference is absent. See each action's field table below.

                Field criticality levels:
                  [CRITICAL]   → Cannot proceed without it. Block and ask if missing.
                  [INFERRED]   → Derive from context, related fields, or message keywords. Never block.
                  [DEFAULTED]  → Use the exact default value listed. Never block. Never ask.
                ════════════════════════════════════════════════════════

                ════════════════════════════════════════════════════════
                ACTION: SEND_MESSAGE
                ────────────────────────────────────────────────────────
                Field           Level        Inference / Default rule
                recipientId     [INFERRED]   Resolve by name/email/role from DATA CONTEXT.
                                             If BY ROLE → expand to list of user IDs.
                                             If ambiguous name → set needsRecipient:true.
                recipientName   [INFERRED]   Use resolved display name(s).
                subject         [INFERRED]   Derive from body content. NEVER block.
                body            [CRITICAL]   Must be present. If absent → ask ONCE.
                senderId        [DEFAULTED]  %s
                priority        [INFERRED]   "urgent"/"immediately" → HIGH, else NORMAL

                MULTI-MESSAGE: If user specifies multiple recipient+message pairs,
                return an array of SEND_MESSAGE payloads inside "messages": [...].

                OUTPUT SCHEMA for multi-message:
                {
                  "actionDetected": true,
                  "actionType": "SEND_MESSAGE",
                  "multiMessage": true,
                  "messages": [
                    { "recipientId": "...", "recipientName": "...", "subject": "...", "body": "...", "priority": "NORMAL" },
                    { "recipientId": "...", "recipientName": "...", "subject": "...", "body": "...", "priority": "NORMAL" }
                  ],
                  "confirmationMessage": "I'll send 2 messages: [1] To Ahmed: '...' [2] To Sarah: '...'. Shall I proceed?"
                }

                BLOCKING RULE: Block only if body is completely absent.
                ════════════════════════════════════════════════════════

                ════════════════════════════════════════════════════════
                ACTION: SUBMIT_PART_REQUEST
                ────────────────────────────────────────────────────────
                Field           Level        Inference / Default rule
                partName        [INFERRED]   Extract from message. "SSD", "500GB SSD", "RAM 8GB",
                                             "storage", "battery", "charger" → use the most specific
                                             noun phrase. If only category given ("storage") → use
                                             that as partName. NEVER block on this field.
                category        [INFERRED]   Derive from partName: SSD/HDD → "Storage",
                                             RAM → "Memory", battery/charger → "Power",
                                             screen → "Display". If unclear → "General"
                equipmentId     [DEFAULTED]  "General" — NEVER ask for this. NEVER block.
                equipmentName   [DEFAULTED]  "Unassigned" — NEVER ask for this. NEVER block.
                reason          [DEFAULTED]  "Requested via AI Assistant" — unless the user gave a
                                             reason, then use verbatim. NEVER ask. NEVER block.
                status          [DEFAULTED]  "PENDING" — always.
                requesterId     [DEFAULTED]  Use %s — always.
                priority        [INFERRED]   "urgent"/"immediately" → HIGH, "soon"/"need" → MEDIUM,
                                             else → LOW
                quantity        [INFERRED]   Extract number if mentioned, else 1.

                BLOCKING RULE for SUBMIT_PART_REQUEST:
                  → NEVER block. partName can always be inferred from context.
                    "request ssd storage" → partName: "SSD Storage", category: "Storage"
                    "I need a part for my laptop" → partName: "Laptop Part (unspecified)", category: "General"
                    "request any part has category storage and type ssd" → partName: "SSD", category: "Storage"
                ════════════════════════════════════════════════════════

                ════════════════════════════════════════════════════════
                ACTION: ADD_EQUIPMENT
                ────────────────────────────────────────────────────────
                Field           Level        Inference / Default rule
                equipmentName   [INFERRED]   Combine brand + type if both present. "Samsung SSD 500GB".
                                             If only type → use type. NEVER block.
                category        [INFERRED]   Use user's explicit category if provided (e.g. "category component").
                                             Else infer: SSD/HDD/NVMe → "Storage", Laptop/PC → "Computing",
                                             RAM/Memory → "Memory", Switch/Router → "Networking",
                                             Monitor/Screen → "Display", Printer → "Printing", else "General IT".
                type            [INFERRED]   Use user's explicit type if provided.
                                             Else most specific hardware noun. (Do NOT use equipmentType).
                brand           [INFERRED]   Extract brand name. If absent → "Unbranded"
                specifications  [INFERRED]   Must be a JSON object with a "details" key. Example: {"details": "500GB SSD"}.
                status          [DEFAULTED]  "AVAILABLE"
                shelfId         [DEFAULTED]  "Unassigned"
                qte             [DEFAULTED]  1
                id              [DEFAULTED]  Generate: "EQ-" + 8 random hex chars (e.g. "EQ-3f8a2c1b")
                serialNumber    [INFERRED]   Use if given. Else generate: "SN-" + 6 uppercase alphanum
                qrCode          [DEFAULTED]  "QR-" + same suffix as serialNumber
                purchasePrice   [INFERRED]   Extract if mentioned (e.g. "costs 250") → 250.0. Else 0.0

                BLOCKING RULE: NEVER block. All fields have inference or default paths.
                ════════════════════════════════════════════════════════

                ════════════════════════════════════════════════════════
                ACTION: DELETE_EQUIPMENT
                ────────────────────────────────────────────────────────
                id OR serialNumber   [CRITICAL]   Search the message for any UUID, numeric ID,
                                                  or SN-xxx pattern. If found → proceed.
                                                  If NOT found → set needsId:true, do NOT execute.

                BLOCKING RULE: Block ONLY if no identifier found at all.
                  Ask: "Which equipment should I delete? Please provide the ID or serial number."
                ════════════════════════════════════════════════════════

                ════════════════════════════════════════════════════════
                ACTION: UPDATE_EQUIPMENT
                ────────────────────────────────────────────────────────
                id               [CRITICAL]   Same as DELETE — must be present.
                fields to update [INFERRED]   Extract all key:value changes from the message.

                BLOCKING RULE: Block only if no ID found.
                ════════════════════════════════════════════════════════

                ════════════════════════════════════════════════════════
                ACTION: APPROVE_REQUEST / REJECT_REQUEST
                ────────────────────────────────────────────────────────
                id               [CRITICAL]   Must find a request ID in the message.
                rejectionReason  [DEFAULTED]  For REJECT: "No reason provided" if absent.

                BLOCKING RULE: Block only if no request ID found.
                ════════════════════════════════════════════════════════

                ════════════════════════════════════════════════════════
                ACTION: CREATE_TASK
                ────────────────────────────────────────────────────────
                title           [INFERRED]   Core task noun phrase from message. NEVER block.
                dueDate         [INFERRED]   Extract any date/time mention. Else null.
                priority        [INFERRED]   Urgency words → HIGH/MEDIUM/LOW. Default LOW.
                status          [DEFAULTED]  "TODO"
                assignedTo      [DEFAULTED]  %s

                BLOCKING RULE: NEVER block. Title can always be inferred.
                ════════════════════════════════════════════════════════

                ════════════════════════════════════════════════════════
                ACTION: UPDATE_TICKET
                ────────────────────────────────────────────────────────
                id              [CRITICAL]   Must be in message.
                fields          [INFERRED]   Extract change intent.

                BLOCKING RULE: Block only if no ticket ID.
                ════════════════════════════════════════════════════════

                ════════════════════════════════════════════════════════
                OUTPUT SCHEMA
                ────────────────────────────────────────────────────────
                When actionDetected = false:
                  {"actionDetected": false}

                When actionDetected = true:
                {
                  "actionDetected": true,
                  "actionType": "ACTION_TYPE_HERE",
                  "needsId": false,
                  "inferredFields": ["list of fields that were defaulted or inferred, not given by user"],
                  "payload": {
                    ...all fields with their resolved values...
                  },
                  "confirmationMessage": "..."
                }

                CONFIRMATION MESSAGE RULES:
                - Be specific: name the actual item/part.
                - If inferredFields is not empty, list what was assumed in one short sentence.
                - Example: "I'll submit a part request for SSD Storage (priority: MEDIUM).
                  I assumed: equipmentId=General, reason=Requested via AI Assistant.
                  Does this look correct?"
                - Never say "I am unable to process" unless needsId:true.
                - Max 3 sentences.
                ════════════════════════════════════════════════════════
                """.formatted(role, userId, userId, userId, userId);

        try {
            OpenAiChatOptions options = OpenAiChatOptions.builder()
                    .model("google/gemini-2.0-flash-001")
                    .build();

            ChatResponse chatResponse = chatModel.call(new Prompt(systemPrompt + "\n\nUser Message: " + message, options));
            if (chatResponse == null || chatResponse.getResult() == null) return false;

            String jsonResponse = chatResponse.getResult().getOutput().getText();
            if (jsonResponse == null) return false;

            // Clean JSON markdown if present
            jsonResponse = jsonResponse.trim();
            if (jsonResponse.startsWith("```json")) {
                jsonResponse = jsonResponse.substring(7, jsonResponse.length() - 3).trim();
            } else if (jsonResponse.startsWith("```")) {
                jsonResponse = jsonResponse.substring(3, jsonResponse.length() - 3).trim();
            }

            Map<String, Object> result = objectMapper.readValue(jsonResponse, new TypeReference<>() {});
            
            if (result != null && Boolean.TRUE.equals(result.get("actionDetected"))) {
                // If the LLM flagged that it needs an ID (critical field missing), do not propose the action yet.
                // The confirmationMessage will contain the prompt asking for the ID.
                if (Boolean.TRUE.equals(result.get("needsId"))) {
                    // We can either return false (fallback to general AI) OR set the AI response directly.
                    // Let's set the AI response directly to the confirmationMessage, so the AI asks for the ID.
                    response.setSuccess(true);
                    response.setIntent("ACTION_MISSING_ID");
                    response.setRole(role);
                    response.setAnswer((String) result.getOrDefault("confirmationMessage", "Please provide the ID."));
                    return true; // We intercepted the chat and provided the question.
                }

                response.setActionPending(true);
                response.setActionType((String) result.get("actionType"));
                response.setActionPayload((Map<String, Object>) result.get("payload"));
                String confirmMsg = (String) result.getOrDefault("confirmationMessage", "I will perform this action.");
                response.setAnswer(confirmMsg + "\n\n⚠️ **This action requires your confirmation. Do you want me to proceed?**");
                log.info("Action detected: {} for role: {}", response.getActionType(), role);
                return true;
            }
        } catch (Exception e) {
            log.error("Failed to detect action: {}", e.getMessage());
        }

        return false;
    }
}
