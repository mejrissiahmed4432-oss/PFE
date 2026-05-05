package com.example.aiservice.controller;

import com.example.aiservice.model.AiRequest;
import com.example.aiservice.model.AiResponse;
import com.example.aiservice.model.QueryIntent;
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

    private static final Set<String> SUPPORTED_ROLES = Set.of(
            "stock_manager", "technician", "admin");

    private final ChatModel chatModel;
    private final IntentService intentService;
    private final QueryRouterService queryRouter;
    private final PromptBuilderService promptBuilder;
    private final RecommendationService recommendationService;
    private final DataIngestionService dataIngestion;
    private final VectorStoreService vectorStore;

    private final ActionDetectorService actionDetector;

    public AiController(ChatModel chatModel,
            IntentService intentService,
            QueryRouterService queryRouter,
            PromptBuilderService promptBuilder,
            RecommendationService recommendationService,
            DataIngestionService dataIngestion,

            VectorStoreService vectorStore,
            ActionDetectorService actionDetector) {

        this.chatModel = chatModel;
        this.intentService = intentService;
        this.queryRouter = queryRouter;
        this.promptBuilder = promptBuilder;
        this.recommendationService = recommendationService;
        this.dataIngestion = dataIngestion;
        this.vectorStore = vectorStore;

        this.actionDetector = actionDetector;

    }

    // ─────────────────────────────────────────────────────────────────────────
    // POST /ai/query
    // ─────────────────────────────────────────────────────────────────────────

    @PostMapping("/query")
    public ResponseEntity<AiResponse> query(@Valid @RequestBody AiRequest request) {
        String role = request.getRole().toLowerCase().trim();
        String message = request.getMessage().trim();
        String userId = request.getUserId();
        List<Map<String, String>> history = request.getConversationHistory();

        log.info("AI query — userId={}, role={}, history={} turns, message='{}'",
                userId, role, history != null ? history.size() : 0, message);

        if (!SUPPORTED_ROLES.contains(role)) {
            return ResponseEntity.badRequest().body(
                    AiResponse.error("Unsupported role: '" + request.getRole() +
                            "'. Supported roles: " + SUPPORTED_ROLES));
        }

        try {

            AiResponse response = new AiResponse();

            // 1. Detect if it's an action (Create/Update/Delete)
            if (actionDetector.detectAndPopulate(message, role, response)) {
                response.setRole(role);
                return ResponseEntity.ok(response);
            }

            // 2. Detect intent

            //
            QueryIntent intent = intentService.detect(message, role);
            log.info("Detected intent: {} for role: {}", intent, role);

            if (!intent.isAccessibleBy(role)) {
                intent = QueryIntent.GENERAL_ASSISTANCE;
                log.warn("Intent not accessible by role, falling back to GENERAL_ASSISTANCE");
            }

            // 2. Route to data source
            String dataContext = queryRouter.route(intent, role, message, userId);

            // 3. Get structured data for response
            List<Map<String, Object>> structuredData = queryRouter.getStructuredData(intent, role, message, userId);

            // 4. Build augmented prompt
            String fullPrompt = promptBuilder.build(intent, role, dataContext, message);

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
