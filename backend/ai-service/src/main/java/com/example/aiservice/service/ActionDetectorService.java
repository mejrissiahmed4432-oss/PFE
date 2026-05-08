package com.example.aiservice.service;

import com.example.aiservice.clients.StockClient;
import com.example.aiservice.clients.UserClient;
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

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Uses LLM to detect if a message is an actionable command (create, update, delete).
 * Injects REAL system data (categories, types, spec fields, shelves) into the prompt.
 */
@Service
public class ActionDetectorService {

    private static final Logger log = LoggerFactory.getLogger(ActionDetectorService.class);
    private final ChatModel chatModel;
    private final StockClient stockClient;
    private final UserClient userClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public ActionDetectorService(ChatModel chatModel, StockClient stockClient, UserClient userClient) {
        this.chatModel = chatModel;
        this.stockClient = stockClient;
        this.userClient = userClient;
    }

    /**
     * Builds a rich category→type→specificationFields mapping for prompt injection.
     * Example output:
     *   COMPONENT → types: RAM (specs: ram), CPU (specs: none), GPU (specs: none)
     *   DEVICE → types: Laptop (specs: cpu, ram), Desktop (specs: none)
     */
    @SuppressWarnings("unchecked")
    private String fetchCategoryTypeSpecMapping() {
        try {
            List<Map<String, Object>> cats = stockClient.getAllCategories();
            if (cats != null && !cats.isEmpty()) {
                StringBuilder sb = new StringBuilder();
                for (Map<String, Object> cat : cats) {
                    String catName = String.valueOf(cat.getOrDefault("name", "Unknown"));
                    String catId = String.valueOf(cat.getOrDefault("id", ""));
                    sb.append("  CATEGORY: ").append(catName).append(" (id: ").append(catId).append(")\n");
                    Object typesObj = cat.get("types");
                    if (typesObj instanceof List) {
                        List<Map<String, Object>> types = (List<Map<String, Object>>) typesObj;
                        sb.append("    Types: ");
                        for (int i = 0; i < types.size(); i++) {
                            Map<String, Object> t = types.get(i);
                            String tName = String.valueOf(t.getOrDefault("name", "Unknown"));
                            Object specsObj = t.get("specificationFields");
                            String specStr = "none";
                            if (specsObj instanceof List) {
                                List<String> specs = (List<String>) specsObj;
                                if (!specs.isEmpty()) {
                                    specStr = String.join(", ", specs);
                                }
                            }
                            sb.append(tName).append(" (specs: ").append(specStr).append(")");
                            if (i < types.size() - 1) sb.append(", ");
                        }
                        sb.append("\n");
                    }
                }
                return sb.toString();
            }
        } catch (Exception e) {
            log.warn("Could not fetch category-type-spec mapping: {}", e.getMessage());
        }
        return "  COMPONENT → types: RAM (specs: ram), CPU (specs: none), GPU (specs: none)\n"
             + "  STORAGE → types: SSD (specs: none), HDD (specs: none)\n"
             + "  DEVICE → types: Laptop (specs: cpu, ram), Desktop (specs: none)\n"
             + "  PERIPHERAL → types: Keyboard (specs: none), Mouse (specs: none), Printer (specs: none)\n"
             + "  NETWORK → types: Router (specs: none), Switch (specs: none)\n";
    }

    /**
     * Fetches shelves that have available space (currentQte < maxQte).
     * Returns a formatted string listing available shelves with their IDs.
     */
    @SuppressWarnings("unchecked")
    private String fetchAvailableShelves() {
        try {
            List<Map<String, Object>> shelves = stockClient.getAllShelves();
            if (shelves != null && !shelves.isEmpty()) {
                StringBuilder sb = new StringBuilder();
                boolean anyAvailable = false;
                for (Map<String, Object> s : shelves) {
                    int current = toInt(s.get("currentQte"), 0);
                    int max = toInt(s.get("maxQte"), 0);
                    if (current < max) {
                        anyAvailable = true;
                        String shelfId = String.valueOf(s.getOrDefault("id", ""));
                        String shelfNb = String.valueOf(s.getOrDefault("nb", "Unknown"));
                        int freeSpace = max - current;
                        sb.append("  Shelf #").append(shelfNb)
                          .append(" (id: ").append(shelfId).append(")")
                          .append(" — current: ").append(current).append("/").append(max)
                          .append(", free space: ").append(freeSpace)
                          .append("\n");
                    }
                }
                if (!anyAvailable) {
                    return "  NO SHELVES AVAILABLE — all shelves are full. BLOCK equipment creation.";
                }
                return sb.toString();
            }
        } catch (Exception e) {
            log.warn("Could not fetch available shelves: {}", e.getMessage());
        }
        return "  Shelf data unavailable. Use shelfId: \"Unassigned\".";
    }

    private int toInt(Object obj, int defaultVal) {
        if (obj == null) return defaultVal;
        if (obj instanceof Number) return ((Number) obj).intValue();
        try { return Integer.parseInt(String.valueOf(obj)); } catch (Exception e) { return defaultVal; }
    }

    /**
     * Fetches all system users for recipient resolution.
     */
    private String fetchSystemUsers() {
        try {
            List<Map<String, Object>> users = userClient.getAllUsers();
            if (users != null && !users.isEmpty()) {
                StringBuilder sb = new StringBuilder();
                for (Map<String, Object> u : users) {
                    String id = String.valueOf(u.getOrDefault("id", ""));
                    String name = u.getOrDefault("firstName", "") + " " + u.getOrDefault("lastName", "");
                    String email = String.valueOf(u.getOrDefault("email", ""));
                    String role = String.valueOf(u.getOrDefault("role", ""));
                    sb.append("  USER: ").append(name.trim())
                      .append(" (id: ").append(id).append(")")
                      .append(" [email: ").append(email).append("]")
                      .append(" [role: ").append(role).append("]\n");
                }
                return sb.toString();
            }
        } catch (Exception e) {
            log.warn("Could not fetch system users: {}", e.getMessage());
        }
        return "  User data unavailable. Ask for recipient name.";
    }

    /**
     * Analyzes the message to see if it triggers a write action.
     * Returns true if an action is detected and updates the AiResponse with payload.
     */
    public boolean detectAndPopulate(String message, String role, String userId, AiResponse response) {
        String lower = message.toLowerCase();
        boolean potentialAction = lower.contains("add") || lower.contains("create") || 
                                 lower.contains("update") || lower.contains("approve") || 
                                 lower.contains("reject") || lower.contains("submit") ||
                                 lower.contains("delete") || lower.contains("remove") ||
                                 lower.contains("send") || lower.contains("message") ||
                                 lower.contains("notify") || lower.contains("tell") ||
                                 lower.contains("supplier") || lower.contains("category") ||
                                 lower.contains("type");

        if (!potentialAction) return false;

        // Build prompt using StringBuilder to avoid .formatted() argument count issues
        String categoryMapping = fetchCategoryTypeSpecMapping();
        String availableShelves = fetchAvailableShelves();

        StringBuilder prompt = new StringBuilder();
        prompt.append("""
                You are an Intent Extraction Engine for an IT Asset Management platform.
                OUTPUT: A single raw JSON object. No markdown. No explanation. No code fences.

                ═══════════════════════════════════════
                CURRENT USER
                  Role    : """).append(role).append("""
                
                  User ID : """).append(userId).append("""
                
                  Today   : """).append(java.time.LocalDate.now().toString()).append("""
                
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
                CREATE_CATEGORY            ✓             ✓            ✗         ✓
                ADD_TYPE                   ✓             ✓            ✗         ✓
                ADD_SUPPLIER               ✓             ✓            ✗         ✓
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
                
                DATA CLEANING:
                  ALWAYS strip leading/trailing quotes, commas, or periods from extracted strings.
                ════════════════════════════════════════════════════════

                ════════════════════════════════════════════════════════
                ACTION: SEND_MESSAGE
                ────────────────────────────────────────────────────────
                SYSTEM USERS (use to resolve receiverId):
                """).append(fetchSystemUsers()).append("""
                
                Field           Level        Inference / Default rule
                receiverId      [CRITICAL]   Resolve by matching user input (name, email, or role)
                                             against the SYSTEM USERS list above.
                                             - If match by Name/Email → return that user's id.
                                             - If match by ROLE (e.g. "tell all technicians")
                                               → return "ROLE:ROLE_NAME" (e.g. "ROLE:TECHNICIAN").
                                             - If user NOT found → set actionDetected:false and ask.
                content         [CRITICAL]   The message text. Must be present. If absent → ask.
                senderId        [DEFAULTED]  """).append(userId).append("""

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
                requesterId     [DEFAULTED]  Use """).append(userId).append("""
                 — always.
                priority        [INFERRED]   "urgent"/"immediately" → HIGH, "soon"/"need" → MEDIUM,
                                             else → LOW
                quantity        [INFERRED]   Extract number if mentioned, else 1.

                BLOCKING RULE for SUBMIT_PART_REQUEST:
                  → NEVER block. partName can always be inferred from context.
                ════════════════════════════════════════════════════════

                ════════════════════════════════════════════════════════
                ACTION: ADD_EQUIPMENT
                ────────────────────────────────────────────────────────
                SYSTEM CATEGORY → TYPE → SPECIFICATION FIELDS MAPPING:
                """).append(categoryMapping).append("""

                AVAILABLE SHELVES WITH FREE SPACE:
                """).append(availableShelves).append("""

                FIELD RULES:
                equipmentName   [INFERRED]   Combine brand + type + key specs.
                                             Examples: "Samsung RAM 16GB DDR4", "Dell Laptop", "WD SSD 500GB".
                                             If only type → use type as name. NEVER block.
                name            [INFERRED]   Same value as equipmentName. Always include BOTH fields.
                category        [INFERRED]   MUST use one of the EXACT CATEGORY NAMES from the mapping above.
                                             Match the hardware type to its parent category.
                                             RAM, CPU, GPU, Motherboard → COMPONENT
                                             SSD, HDD, NVMe, USB Flash → STORAGE
                                             Laptop, Desktop, Server, Smartphone → DEVICE
                                             Monitor, Keyboard, Mouse, Printer → PERIPHERAL
                                             Router, Switch, Access Point → NETWORK
                                             If no match → use first category in the list.
                type            [INFERRED]   MUST use one of the EXACT TYPE NAMES from the mapping above.
                                             Match user input to existing types (case-insensitive).
                                             "Ram" → "RAM", "ssd" → "SSD", "laptop" → "Laptop"
                brand           [INFERRED]   Extract brand name. Known: Dell, HP, Samsung, Lenovo, Apple,
                                             Asus, Acer, WD, Seagate, Kingston, Corsair, LG.
                                             If not found → "Unbranded".
                specifications  [INFERRED]   CRITICAL: Use ONLY the specificationFields defined for the
                                             matched type in the mapping above.
                                             Example: RAM type has specs: ["ram"]
                                               → specifications: {"ram": "16GB DDR4"}
                                             Example: Laptop type has specs: ["cpu", "ram"]
                                               → specifications: {"cpu": "i7-12700H", "ram": "16GB DDR5"}
                                             If the type has NO specificationFields (specs: none),
                                               → specifications: {} (empty object)
                                             NEVER invent new spec field names like "capacity", "details",
                                             "standard", "interface". ONLY use the exact field names
                                             from the type's specificationFields list.
                status          [DEFAULTED]  "Available"
                shelfId         [INFERRED]   MUST assign to an available shelf from the list above.
                                             Pick the FIRST shelf with free space.
                                             If NO shelves have free space → set actionDetected:false
                                             and return confirmationMessage: "Cannot add equipment: all
                                             shelves are full. Please free up shelf space first."
                qte             [INFERRED]   Extract quantity if mentioned (e.g. "5 units" → 5). Else 1.
                                             IMPORTANT: qte must NOT exceed the shelf's free space.
                serialNumber    [INFERRED]   Use exactly if user provides one.
                                             Else auto-generate: 10 random alphanumeric characters
                                             (mix of UPPERCASE letters and digits, NO prefix, NO dashes).
                                             Total length MUST be EXACTLY 10 characters.
                                             Examples: "A3BF2C1K9M", "K7M2P9X4T8", "R8T4Q6W3J5"
                qrCode          [DEFAULTED]  Same value as serialNumber.
                id              [DEFAULTED]  Generate: "EQ-" + 8 random hex chars (e.g. "EQ-3f8a2c1b")
                purchasePrice   [INFERRED]   Extract if mentioned (e.g. "costs 250", "250 DT") → 250.0. Else 0.0.
                nature          [DEFAULTED]  "Asset"
                createdBy       [DEFAULTED]  """).append(userId).append("""


                BLOCKING RULE: Block ONLY if all shelves are full (no free space).
                ════════════════════════════════════════════════════════

                ════════════════════════════════════════════════════════
                ACTION: CREATE_CATEGORY
                ────────────────────────────────────────────────────────
                Field           Level        Inference / Default rule
                name            [CRITICAL]   Category name from user message. UPPERCASE format.
                                             Examples: "COMPONENT", "STORAGE", "NETWORK"
                icon            [INFERRED]   Match to lucide icon name. Examples:
                                             hardware→cpu, storage→hard-drive, network→wifi,
                                             peripheral→mouse, device→monitor. Default: "box"
                types           [DEFAULTED]  Empty array [] — types are added separately.

                BLOCKING RULE: Block only if no category name can be extracted.
                ════════════════════════════════════════════════════════

                ════════════════════════════════════════════════════════
                ACTION: ADD_TYPE
                ────────────────────────────────────────────────────────
                EXISTING CATEGORIES (use categoryId from this list):
                """).append(categoryMapping).append("""

                Field           Level        Inference / Default rule
                categoryId      [CRITICAL]   MUST match an existing category ID from the list above.
                                             User says "add type SSD to STORAGE" → find STORAGE's id.
                                             If category not found → block and ask.
                name            [CRITICAL]   Type name from user message. Title case.
                                             Examples: "SSD", "RAM", "Laptop", "Router"
                requiresQrCode  [INFERRED]   Large/expensive items (Laptop, Server, Desktop) → true.
                                             Small items (Cable, Adapter) → false. Default: false.
                specificationFields [INFERRED] Extract any spec field names mentioned.
                                             Examples: "with specs ram and cpu" → ["ram", "cpu"]
                                             Default: [] (empty array)

                BLOCKING RULE: Block if no category can be matched or no type name given.
                ════════════════════════════════════════════════════════

                ════════════════════════════════════════════════════════
                ACTION: ADD_SUPPLIER
                ────────────────────────────────────────────────────────
                Field           Level        Inference / Default rule
                companyName     [CRITICAL]   Company name from user message. Block if missing.
                address         [INFERRED]   Extract if mentioned. Default: "Not specified"
                phoneNumber     [INFERRED]   Extract if mentioned. Default: "+21600000000"
                email           [INFERRED]   Extract if mentioned. Default: "not-set@company.com"
                website         [INFERRED]   Extract if mentioned. MUST be a valid URL.
                                             If user gives "example.com" or "https:example.com"
                                             → output "https://example.com".
                                             Default: "https://www.company.com"
                category        [INFERRED]   "Hardware", "Software", "Services". Default: "Hardware"
                contactPerson   [INFERRED]   Extract the name of the individual or contact point.
                                             User says "contact named X" or "person named X" → X.
                                             Default: "Not specified"
                rating          [DEFAULTED]  5
                note            [INFERRED]   Extract if mentioned. Default: "Added via AI Assistant"

                BLOCKING RULE: Block only if company name is completely absent.
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
                Field           Level        Inference / Default rule
                title           [INFERRED]   Core task noun phrase from message. NEVER block.
                description     [INFERRED]   Expand on title if possible. Default: same as title.
                type            [INFERRED]   "Maintenance","Equipment","Stock","General".
                                             Default: "General"
                dueDate         [INFERRED]   MUST be in YYYY-MM-DD format (date only, NO time part).
                                             "today" → """).append(java.time.LocalDate.now().toString()).append("""
                
                                             "tomorrow" → """).append(java.time.LocalDate.now().plusDays(1).toString()).append("""
                
                                             "next week" → """).append(java.time.LocalDate.now().plusWeeks(1).toString()).append("""
                
                                             If no date mentioned → use today's date.
                                             CRITICAL: Format MUST be YYYY-MM-DD (e.g. "2026-05-08").
                                             NEVER add time part like T00:00:00.
                priority        [INFERRED]   "High", "Medium", "Low". Default "Medium".
                status          [DEFAULTED]  "Pending"
                assignedTo      [DEFAULTED]  """).append(userId).append("""


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
                """);

        String systemPrompt = prompt.toString();

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
                if (Boolean.TRUE.equals(result.get("needsId"))) {
                    response.setSuccess(true);
                    response.setIntent("ACTION_MISSING_ID");
                    response.setRole(role);
                    response.setAnswer((String) result.getOrDefault("confirmationMessage", "Please provide the ID."));
                    return true;
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

