package com.medina.app.model;

import java.util.Date;
import java.util.List;
import java.util.Map;

public class AiChatMessage {
    private long id;
    private String text;
    private String sender;
    private Date timestamp;
    private List<String> suggestions;
    private List<Map<String, Object>> data;
    private boolean isError;
    private boolean actionPending;
    private String actionType;
    private Map<String, Object> actionPayload;
    private String imageUrl;

    public AiChatMessage() {}

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public String getText() { return text; }
    public void setText(String text) { this.text = text; }

    public String getSender() { return sender; }
    public void setSender(String sender) { this.sender = sender; }

    public Date getTimestamp() { return timestamp; }
    public void setTimestamp(Date timestamp) { this.timestamp = timestamp; }

    public List<String> getSuggestions() { return suggestions; }
    public void setSuggestions(List<String> suggestions) { this.suggestions = suggestions; }

    public List<Map<String, Object>> getData() { return data; }
    public void setData(List<Map<String, Object>> data) { this.data = data; }

    public boolean isError() { return isError; }
    public void setError(boolean error) { isError = error; }

    public boolean isActionPending() { return actionPending; }
    public void setActionPending(boolean actionPending) { this.actionPending = actionPending; }

    public String getActionType() { return actionType; }
    public void setActionType(String actionType) { this.actionType = actionType; }

    public Map<String, Object> getActionPayload() { return actionPayload; }
    public void setActionPayload(Map<String, Object> actionPayload) { this.actionPayload = actionPayload; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
}
