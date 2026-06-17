package com.medina.app.model;

import java.util.List;

public class AiRequest {
    private String userId;
    private String role;
    private String message;
    private List<ConversationTurn> conversationHistory;
    private String imageBase64;

    public AiRequest() {}

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public List<ConversationTurn> getConversationHistory() { return conversationHistory; }
    public void setConversationHistory(List<ConversationTurn> conversationHistory) { this.conversationHistory = conversationHistory; }

    public String getImageBase64() { return imageBase64; }
    public void setImageBase64(String imageBase64) { this.imageBase64 = imageBase64; }
}
