package com.medina.app.model;

import java.util.Date;
import java.util.List;

public class AiConversation {
    private String id;
    private String userId;
    private String title;
    private Date createdAt;
    private Date updatedAt;
    private List<AiChatMessage> messages;

    public AiConversation() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public Date getCreatedAt() { return createdAt; }
    public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }

    public Date getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Date updatedAt) { this.updatedAt = updatedAt; }

    public List<AiChatMessage> getMessages() { return messages; }
    public void setMessages(List<AiChatMessage> messages) { this.messages = messages; }
}
