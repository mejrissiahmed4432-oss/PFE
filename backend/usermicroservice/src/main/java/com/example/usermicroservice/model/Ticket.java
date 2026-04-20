package com.example.usermicroservice.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Document(collection = "tickets")
public class Ticket {
    @Id
    private String id;
    private String title;
    private String description;
    private String category; // "Maintenance", "Inspection", "Incident"
    private String priority; // "High", "Medium", "Low"
    private String status;   // "Open", "In Progress", "Resolved", "Closed" ...
    private String userId;   // Creator's ID (createdBy)
    private String userName; // Creator's name
    private String userRole; // Creator's role
    private String assignedTo; // Assigned technician ID
    private String equipmentName; // Equipment this ticket is for
    private String deadline;     // Optional deadline date
    private List<String> attachments; // Base64 or URLs of attachments
    private String workNote;
    private List<java.util.Map<String, Object>> repairTasks;
    private List<java.util.Map<String, Object>> partsUsed;
    private String createdAt;
    private String updatedAt;

    public Ticket() {}

    public void prePersist() {
        String now = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        if (this.createdAt == null) {
            this.createdAt = now;
        }
        if (this.status == null) {
            this.status = "Open";
        }
        if (this.priority == null) {
            this.priority = "Medium";
        }
        this.updatedAt = now;
    }

    public void preUpdate() {
        this.updatedAt = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public String getPriority() { return priority; }
    public void setPriority(String priority) { this.priority = priority; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public String getUserName() { return userName; }
    public void setUserName(String userName) { this.userName = userName; }
    public String getUserRole() { return userRole; }
    public void setUserRole(String userRole) { this.userRole = userRole; }
    public String getAssignedTo() { return assignedTo; }
    public void setAssignedTo(String assignedTo) { this.assignedTo = assignedTo; }
    public String getEquipmentName() { return equipmentName; }
    public void setEquipmentName(String equipmentName) { this.equipmentName = equipmentName; }
    public String getDeadline() { return deadline; }
    public void setDeadline(String deadline) { this.deadline = deadline; }
    public List<String> getAttachments() { return attachments; }
    public void setAttachments(List<String> attachments) { this.attachments = attachments; }
    public String getWorkNote() { return workNote; }
    public void setWorkNote(String workNote) { this.workNote = workNote; }
    public List<java.util.Map<String, Object>> getRepairTasks() { return repairTasks; }
    public void setRepairTasks(List<java.util.Map<String, Object>> repairTasks) { this.repairTasks = repairTasks; }
    public List<java.util.Map<String, Object>> getPartsUsed() { return partsUsed; }
    public void setPartsUsed(List<java.util.Map<String, Object>> partsUsed) { this.partsUsed = partsUsed; }
    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
    public String getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(String updatedAt) { this.updatedAt = updatedAt; }
}
