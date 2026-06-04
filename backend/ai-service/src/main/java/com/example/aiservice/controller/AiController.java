package com.example.aiservice.controller;

import com.example.aiservice.model.AiRequest;
import com.example.aiservice.model.AiResponse;
import com.example.aiservice.model.QueryIntent;
import com.example.aiservice.model.EquipmentParsingRequest;
import com.example.aiservice.model.EquipmentParsingResponse;
import com.example.aiservice.model.EquipmentSuggestionRequest;
import com.example.aiservice.model.ParsedItem;
import com.example.aiservice.model.AutocompleteRequest;
import com.example.aiservice.model.QuotationAnalysisRequest;
import com.example.aiservice.model.QuotationAnalysisResponse;
import com.example.aiservice.service.*;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;

import org.springframework.ai.model.Media;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.ResponseEntity;
import org.springframework.util.MimeTypeUtils;

import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * Main AI REST controller.
 *
 * POST /ai/query — Ask the AI assistant (supports multi-turn conversation
 * history)
 * POST /ai/reindex — Rebuild the vector store from live data
 * GET /ai/health — Health check + vector store stats
 */
@RestController
@RequestMapping("/ai")
@CrossOrigin(origins = "*")
public class AiController {

    private static final Logger log = LoggerFactory.getLogger(AiController.class);

    // Supported roles (normalised to lowercase with underscores)
    // IT_MANAGER maps to "it_manager" and shares stock_manager level access
    private static final Set<String> SUPPORTED_ROLES = Set.of(

            "stock_manager", "it_manager", "technician", "admin"
    );


    private final ChatModel chatModel;
    private final IntentService intentService;
    private final QueryRouterService queryRouter;
    private final PromptBuilderService promptBuilder;
    private final RecommendationService recommendationService;
    private final DataIngestionService dataIngestion;
    private final VectorStoreService vectorStore;

    private final ActionDetectorService actionDetector;
    private final EquipmentParsingService equipmentParsingService;
    private final PredictiveMaintenanceService predictiveMaintenanceService;

    public AiController(ChatModel chatModel,
            IntentService intentService,
            QueryRouterService queryRouter,
            PromptBuilderService promptBuilder,
            RecommendationService recommendationService,
            DataIngestionService dataIngestion,

            VectorStoreService vectorStore,
            ActionDetectorService actionDetector,
            EquipmentParsingService equipmentParsingService,
            PredictiveMaintenanceService predictiveMaintenanceService) {

        this.chatModel = chatModel;
        this.intentService = intentService;
        this.queryRouter = queryRouter;
        this.promptBuilder = promptBuilder;
        this.recommendationService = recommendationService;
        this.dataIngestion = dataIngestion;
        this.vectorStore = vectorStore;

        this.actionDetector = actionDetector;
        this.equipmentParsingService = equipmentParsingService;
        this.predictiveMaintenanceService = predictiveMaintenanceService;

    }

    // ─────────────────────────────────────────────────────────────────────────
    // POST /ai/query
    // ─────────────────────────────────────────────────────────────────────────

    @PostMapping("/query")
    public ResponseEntity<AiResponse> query(@Valid @RequestBody AiRequest request) {
        // Normalise role: "STOCK_MANAGER" → "stock_manager", "IT_MANAGER" → "it_manager"
        String role    = request.getRole().toLowerCase().trim().replace(" ", "_");
        // Treat IT_MANAGER as stock_manager for intent/data routing purposes
        String routingRole = role.equals("it_manager") ? "stock_manager" : role;
        String message = request.getMessage().trim();
        String userId = request.getUserId();
        List<Map<String, String>> history = request.getConversationHistory();

        log.info("AI query — userId={}, role={}, routingRole={}, history={} turns, message='{}'",
                userId, role, routingRole, history != null ? history.size() : 0, message);

        if (!SUPPORTED_ROLES.contains(role)) {
            return ResponseEntity.badRequest().body(
                    AiResponse.error("Unsupported role: '" + request.getRole() +
                            "'. Supported roles: " + SUPPORTED_ROLES));
        }

        try {

            AiResponse response = new AiResponse();

            // 1. Detect if it's an action (Create/Update/Delete) — use original role for permission check
            if (actionDetector.detectAndPopulate(message, role, userId, response)) {
                response.setRole(role);
                return ResponseEntity.ok(response);
            }


            // 2. Detect intent — use routingRole (IT_MANAGER → stock_manager)
            QueryIntent intent = intentService.detect(message, routingRole);
            log.info("Detected intent: {} for routingRole: {}", intent, routingRole);

            if (!intent.isAccessibleBy(routingRole)) {
                intent = QueryIntent.GENERAL_ASSISTANCE;
                log.warn("Intent not accessible by role, falling back to GENERAL_ASSISTANCE");
            }

            // 3. Route to data source
            String dataContext = queryRouter.route(intent, routingRole, message, userId);

            // 4. Get structured data for response
            List<Map<String, Object>> structuredData = queryRouter.getStructuredData(intent, routingRole, message, userId);

            // 5. Build augmented prompt
            String fullPrompt = promptBuilder.build(intent, routingRole, dataContext, message);

            // 5. Call LLM with conversation history for multi-turn memory

            String answer = callLLM(fullPrompt, history, request.getImageBase64());

            // 6. Generate suggestions
            List<String> suggestions = recommendationService.generate(intent, role, answer, structuredData);

            // 7. Build and return response

            response.setIntent(intent.name());
            response.setAnswer(answer);
            response.setData(structuredData);
            response.setSuggestions(suggestions);
            response.setRole(role);
            response.setSuccess(true);

            log.info("AI response generated for intent={}", intent);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("AI query failed: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body(
                    AiResponse.error("AI service encountered an error: " + e.getMessage()));
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // POST /ai/reindex
    // ─────────────────────────────────────────────────────────────────────────

    @PostMapping("/reindex")
    public ResponseEntity<Map<String, Object>> reindex() {
        log.info("Manual reindex triggered");
        try {
            Map<String, Object> report = dataIngestion.reindex();
            return ResponseEntity.ok(report);
        } catch (Exception e) {
            log.error("Reindex failed: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body(
                    Map.of("status", "error", "message", e.getMessage()));
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // GET /ai/health
    // ─────────────────────────────────────────────────────────────────────────

    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        Map<String, Object> status = new LinkedHashMap<>();
        status.put("service", "ai-service");
        status.put("status", "UP");
        status.put("supportedRoles", SUPPORTED_ROLES);
        status.put("vectorStore", vectorStore.getStats());
        return ResponseEntity.ok(status);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // GET /ai/intents?role=stock_manager
    // ─────────────────────────────────────────────────────────────────────────

    @GetMapping("/intents")
    public ResponseEntity<Map<String, Object>> getIntentsForRole(
            @RequestParam(defaultValue = "stock_manager") String role) {
        Set<QueryIntent> intents = QueryIntent.forRole(role.toLowerCase());
        return ResponseEntity.ok(Map.of(
                "role", role,
                "intents", intents.stream().map(Enum::name).toList()));
    }

    @PostMapping("/parse-equipment-request")
    public ResponseEntity<EquipmentParsingResponse> parseEquipmentRequest(@Valid @RequestBody EquipmentParsingRequest request) {
        log.info("Parsing equipment request text: '{}'", request.getText());
        try {
            List<ParsedItem> items = equipmentParsingService.parseEquipmentRequest(request.getText());
            return ResponseEntity.ok(new EquipmentParsingResponse(items, true));
        } catch (Exception e) {
            log.error("Failed to parse equipment request: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body(
                    EquipmentParsingResponse.error("Parsing failed: " + e.getMessage())
            );
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // POST /ai/suggest-equipment
    // ─────────────────────────────────────────────────────────────────────────

    @PostMapping("/suggest-equipment")
    public ResponseEntity<List<String>> suggestEquipment(@RequestBody EquipmentSuggestionRequest request) {
        log.info("Suggesting equipment for cart with {} items", request.getCartItems() != null ? request.getCartItems().size() : 0);
        try {
            if (request.getCartItems() == null || request.getCartItems().isEmpty()) {
                return ResponseEntity.ok(Collections.emptyList());
            }

            StringBuilder cartContext = new StringBuilder();
            for (EquipmentSuggestionRequest.CartItem item : request.getCartItems()) {
                cartContext.append("- ").append(item.getName());
                if (item.getSelectedSpecs() != null && !item.getSelectedSpecs().isEmpty()) {
                    cartContext.append(" (").append(item.getSelectedSpecs().toString()).append(")");
                }
                cartContext.append("\n");
            }

            List<String> suggestions = equipmentParsingService.suggestRelatedEquipment(cartContext.toString());
            return ResponseEntity.ok(suggestions);
        } catch (Exception e) {
            log.error("Failed to suggest equipment: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @PostMapping("/autocomplete-specs")
    public ResponseEntity<List<String>> autocompleteSpecs(@RequestBody AutocompleteRequest request) {
        if (request.getText() == null || request.getText().trim().isEmpty()) {
            return ResponseEntity.ok(Collections.emptyList());
        }
        
        List<String> completions = equipmentParsingService.autocompleteSpecsWithAI(request.getText());
        return ResponseEntity.ok(completions);
    }

    @PostMapping("/predict-maintenance")
    public ResponseEntity<com.example.aiservice.model.PredictiveMaintenanceResponse> predictMaintenance(@RequestBody com.example.aiservice.model.PredictiveMaintenanceRequest request) {
        log.info("Generating predictive maintenance report for equipment: {}", request.getEquipmentId());
        return ResponseEntity.ok(predictiveMaintenanceService.predict(request));
    }

    @PostMapping("/compare-quotations")
    public ResponseEntity<QuotationAnalysisResponse> compareQuotations(@RequestBody QuotationAnalysisRequest request) {
        log.info("Comparing quotations for request: '{}'", request.getRequestNotes());
        return ResponseEntity.ok(equipmentParsingService.compareQuotations(request));
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    /**
     * Calls the LLM with:
     * 1. A system prompt (data context + instructions)
     * 2. Optional conversation history (prior turns for multi-turn memory)
     * 3. The current user message
     */

    private String callLLM(String fullPrompt, List<Map<String, String>> history, String imageBase64) {

        try {
            // Split system context from user question
            String[] parts = fullPrompt.split("=== USER QUESTION ===\n", 2);
            String systemPart = parts.length > 1 ? parts[0].trim() : "";
            String userPart = parts.length > 1 ? parts[1].trim() : fullPrompt;

            List<Message> messages = new ArrayList<>();

            // 1. System instruction
            messages.add(new SystemMessage(systemPart));

            // 2. Prior conversation turns (multi-turn memory — last 6 turns max)
            if (history != null && !history.isEmpty()) {
                int start = Math.max(0, history.size() - 6);
                for (int i = start; i < history.size(); i++) {
                    Map<String, String> turn = history.get(i);
                    String turnRole = turn.getOrDefault("role", "");
                    String turnContent = turn.getOrDefault("content", "");
                    if ("user".equalsIgnoreCase(turnRole)) {
                        messages.add(new UserMessage(turnContent));
                    } else if ("assistant".equalsIgnoreCase(turnRole)) {
                        messages.add(new AssistantMessage(turnContent));
                    }
                }
            }

            // 3. Current user message & dynamic Vision routing
            UserMessage finalUserMessage;
            OpenAiChatOptions options = null;

            if (imageBase64 != null && !imageBase64.isBlank()) {
                // Remove data URL prefix if present (e.g. data:image/jpeg;base64,...)
                String base64Data = imageBase64.contains(",") ? imageBase64.split(",")[1] : imageBase64;
                byte[] imageBytes = Base64.getDecoder().decode(base64Data);

                Media media = new Media(MimeTypeUtils.IMAGE_JPEG, new ByteArrayResource(imageBytes));
                finalUserMessage = new UserMessage(userPart, List.of(media));

                options = OpenAiChatOptions.builder()
                        .model("google/gemini-2.0-flash-001")
                        .build();
                log.info("Image detected. Routing to vision model: google/gemini-2.0-flash-001");
            } else {
                finalUserMessage = new UserMessage(userPart);
            }
            messages.add(finalUserMessage);

            Prompt prompt = (options != null) ? new Prompt(messages, options) : new Prompt(messages);

            String response = chatModel.call(prompt)

                    .getResult()
                    .getOutput()
                    .getText();

            return (response != null && !response.isBlank())
                    ? response.trim()
                    : "No sufficient data available to answer this question.";

        } catch (Exception e) {
            log.error("LLM call failed: {}", e.getMessage());
            return "The AI model is currently unavailable. Please ensure the model service is running.";
        }
    }
}
