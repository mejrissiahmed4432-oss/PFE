package com.example.aiservice.model;

import jakarta.validation.constraints.NotBlank;
import java.util.List;
import java.util.Map;

public class AiRequest {

    @NotBlank(message = "userId is required")
    private String userId;

    @NotBlank(message = "role is required")
    private String role;   // "stock_manager" | "technician" | "admin"

    @NotBlank(message = "message is required")
    private String message;

    private String conversationId;

    /**
     * Last N messages from the conversation for multi-turn memory.
     * Each entry: {"role": "user"|"assistant", "content": "..."}
     */
    private List<Map<String, String>> conversationHistory;

    /**
     * Optional base64 encoded image to trigger the vision model.
     */
    private String imageBase64;

    // ── Constructors ─────────────────────────────────────────────────────────
    public AiRequest() {}

    // ── Getters / Setters ────────────────────────────────────────────────────
    public String getUserId()           { return userId; }
    public void setUserId(String v)     { this.userId = v; }

    public String getRole()             { return role; }
    public void setRole(String v)       { this.role = v; }

    public String getMessage()          { return message; }
    public void setMessage(String v)    { this.message = v; }

    public String getConversationId()           { return conversationId; }
    public void setConversationId(String v)     { this.conversationId = v; }

    public List<Map<String, String>> getConversationHistory() { return conversationHistory; }
    public void setConversationHistory(List<Map<String, String>> v) { this.conversationHistory = v; }

    public String getImageBase64() { return imageBase64; }
    public void setImageBase64(String v) { this.imageBase64 = v; }
}
