package com.example.usermicroservice.dto;

import java.util.List;

/**
 * DTO used by the IT Manager to create and assign a task to one or more users.
 */
public class TaskAssignRequest {
    private String title;
    private String description;
    private String priority;      // LOW, MEDIUM, HIGH
    private String type;          // Category / type of task
    private String dueDate;
    private String assignedByUserId;   // IT Manager user id
    private List<String> assignedUserIds;  // Target user ids

    public TaskAssignRequest() {}

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getPriority() { return priority; }
    public void setPriority(String priority) { this.priority = priority; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getDueDate() { return dueDate; }
    public void setDueDate(String dueDate) { this.dueDate = dueDate; }

    public String getAssignedByUserId() { return assignedByUserId; }
    public void setAssignedByUserId(String assignedByUserId) { this.assignedByUserId = assignedByUserId; }

    public List<String> getAssignedUserIds() { return assignedUserIds; }
    public void setAssignedUserIds(List<String> assignedUserIds) { this.assignedUserIds = assignedUserIds; }
}
