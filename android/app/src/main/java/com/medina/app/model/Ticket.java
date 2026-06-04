package com.medina.app.model;

public class Ticket {
    private String id;
    private String ticketNumber;
    private String title;
    private String description;
    private String status;
    private String priority;
    private String category;
    private String assignedTo;
    private String technicianName;
    private String deadline;
    private String workNote;
    private String userId;
    private String userName;
    private String userRole;
    private String createdAt;
    private String equipmentId;
    private String equipmentName;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getTicketNumber() { return ticketNumber; }
    public void setTicketNumber(String ticketNumber) { this.ticketNumber = ticketNumber; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getPriority() { return priority; }
    public void setPriority(String priority) { this.priority = priority; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getAssignedTo() { return assignedTo; }
    public void setAssignedTo(String assignedTo) { this.assignedTo = assignedTo; }

    public String getTechnicianName() { return technicianName; }
    public void setTechnicianName(String technicianName) { this.technicianName = technicianName; }

    public String getDeadline() { return deadline; }
    public void setDeadline(String deadline) { this.deadline = deadline; }

    public String getWorkNote() { return workNote; }
    public void setWorkNote(String workNote) { this.workNote = workNote; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getUserName() { return userName; }
    public void setUserName(String userName) { this.userName = userName; }

    public String getUserRole() { return userRole; }
    public void setUserRole(String userRole) { this.userRole = userRole; }

    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }

    public String getEquipmentId() { return equipmentId; }
    public void setEquipmentId(String equipmentId) { this.equipmentId = equipmentId; }

    public String getEquipmentName() { return equipmentName; }
    public void setEquipmentName(String equipmentName) { this.equipmentName = equipmentName; }

    private java.util.List<java.util.Map<String, Object>> repairTasks;
    private java.util.List<java.util.Map<String, Object>> partsUsed;

    public java.util.List<java.util.Map<String, Object>> getRepairTasks() { return repairTasks; }
    public void setRepairTasks(java.util.List<java.util.Map<String, Object>> repairTasks) { this.repairTasks = repairTasks; }

    public java.util.List<java.util.Map<String, Object>> getPartsUsed() { return partsUsed; }
    public void setPartsUsed(java.util.List<java.util.Map<String, Object>> partsUsed) { this.partsUsed = partsUsed; }
}
