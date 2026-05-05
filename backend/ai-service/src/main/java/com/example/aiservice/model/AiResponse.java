package com.example.aiservice.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class AiResponse {

    private String intent;
    private String answer;
    private List<Map<String, Object>> data = new ArrayList<>();
    private List<String> suggestions = new ArrayList<>();
    private String role;
    private boolean success = true;
    private String errorMessage;

<<<<<<< HEAD
    // ── Action fields ─────────────────────────────────────────────────────────
    private boolean actionPending = false;
    private String actionType;
    private Map<String, Object> actionPayload;

=======
>>>>>>> my-local-work
    // ── Static factory methods ────────────────────────────────────────────────
    public static AiResponse success(String intent, String answer,
                                     List<Map<String, Object>> data,
                                     List<String> suggestions, String role) {
        AiResponse r = new AiResponse();
        r.intent = intent;
        r.answer = answer;
        r.data = data != null ? data : new ArrayList<>();
        r.suggestions = suggestions != null ? suggestions : new ArrayList<>();
        r.role = role;
        r.success = true;
        return r;
    }

    public static AiResponse error(String errorMessage) {
        AiResponse r = new AiResponse();
        r.success = false;
        r.errorMessage = errorMessage;
        r.answer = errorMessage;
        return r;
    }

    // ── Getters / Setters ─────────────────────────────────────────────────────
    public String getIntent()               { return intent; }
    public void setIntent(String v)         { this.intent = v; }

    public String getAnswer()               { return answer; }
    public void setAnswer(String v)         { this.answer = v; }

    public List<Map<String, Object>> getData()          { return data; }
    public void setData(List<Map<String, Object>> v)    { this.data = v; }

    public List<String> getSuggestions()            { return suggestions; }
    public void setSuggestions(List<String> v)      { this.suggestions = v; }

    public String getRole()             { return role; }
    public void setRole(String v)       { this.role = v; }

    public boolean isSuccess()          { return success; }
    public void setSuccess(boolean v)   { this.success = v; }

    public String getErrorMessage()             { return errorMessage; }
    public void setErrorMessage(String v)       { this.errorMessage = v; }
<<<<<<< HEAD

    public boolean isActionPending()            { return actionPending; }
    public void setActionPending(boolean v)     { this.actionPending = v; }

    public String getActionType()               { return actionType; }
    public void setActionType(String v)         { this.actionType = v; }

    public Map<String, Object> getActionPayload()            { return actionPayload; }
    public void setActionPayload(Map<String, Object> v)      { this.actionPayload = v; }
=======
>>>>>>> my-local-work
}
