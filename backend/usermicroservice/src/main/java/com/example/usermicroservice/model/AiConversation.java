package com.example.usermicroservice.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Date;
import java.util.List;

@Document(collection = "ai_conversations")
public class AiConversation {

    @Id
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
