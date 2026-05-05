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
    public boolean detectAndPopulate(String message, String role, AiResponse response) {
        // Only trigger for specific keywords to save on API calls
        String lower = message.toLowerCase();
        boolean potentialAction = lower.contains("add") || lower.contains("create") || 
                                 lower.contains("update") || lower.contains("approve") || 
                                 lower.contains("reject") || lower.contains("submit") ||
                                 lower.contains("delete");

        if (!potentialAction) return false;

        String systemPrompt = """
                You are an Intent Extraction Engine.
                Your task is to detect if the user wants to PERFORM an action (create, update, approve, reject).
                
                SUPPORTED ACTIONS:
                - ADD_EQUIPMENT: user wants to add a new device/component.
                - UPDATE_EQUIPMENT: user wants to change info of existing device.
                - SUBMIT_PART_REQUEST: user wants to request a spare part.
                - APPROVE_REQUEST: user (manager) wants to approve a request.
                - REJECT_REQUEST: user (manager) wants to reject a request.
                - CREATE_TASK: user wants to add a task to their schedule.
                - UPDATE_TICKET: user wants to update a maintenance ticket.

                RULES:
                - If NO action is detected, return JSON: {"actionDetected": false}
                - If an action IS detected, return JSON:
                  {
                    "actionDetected": true,
                    "actionType": "THE_TYPE",
                    "payload": { ... extracted fields ... },
                    "confirmationMessage": "A short summary of what you will do"
                  }
                - FOR UPDATE/APPROVE/REJECT: You MUST try to find an 'id' or 'serialNumber' in the message or context.
                - RETURN ONLY RAW JSON. No markdown, no text.
                """;

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
                response.setActionPending(true);
                response.setActionType((String) result.get("actionType"));
                response.setActionPayload((Map<String, Object>) result.get("payload"));
                String confirmMsg = (String) result.getOrDefault("confirmationMessage", "I will perform this action.");
                response.setAnswer(confirmMsg + " **Do you want me to proceed?**");
                log.info("Action detected: {}", response.getActionType());
                return true;
            }
        } catch (Exception e) {
            log.error("Failed to detect action: {}", e.getMessage());
        }

        return false;
    }
}
