package com.medina.app.model;

import java.util.List;
import java.util.Map;

public class AiResponse {
    private String intent;
    private String answer;
    private List<Map<String, Object>> data;
    private List<String> suggestions;
    private String role;
    private boolean success;
    private String errorMessage;
    private boolean actionPending;
    private String actionType;
    private Map<String, Object> actionPayload;

    public AiResponse() {}

    public String getIntent() { return intent; }
    public void setIntent(String intent) { this.intent = intent; }

    public String getAnswer() { return answer; }
    public void setAnswer(String answer) { this.answer = answer; }

    public List<Map<String, Object>> getData() { return data; }
    public void setData(List<Map<String, Object>> data) { this.data = data; }

    public List<String> getSuggestions() { return suggestions; }
    public void setSuggestions(List<String> suggestions) { this.suggestions = suggestions; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }

    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }

    public boolean isActionPending() { return actionPending; }
    public void setActionPending(boolean actionPending) { this.actionPending = actionPending; }

    public String getActionType() { return actionType; }
    public void setActionType(String actionType) { this.actionType = actionType; }

    public Map<String, Object> getActionPayload() { return actionPayload; }
    public void setActionPayload(Map<String, Object> actionPayload) { this.actionPayload = actionPayload; }
}
