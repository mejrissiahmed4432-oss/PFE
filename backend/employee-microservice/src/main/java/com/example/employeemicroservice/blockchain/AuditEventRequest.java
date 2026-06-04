package com.example.employeemicroservice.blockchain;

public class AuditEventRequest {
    private String userId;
    private String userName;
    private String userRole;
    private String action;
    private String details;
    private String ipAddress;

    public AuditEventRequest() {}

    public AuditEventRequest(String userId, String userName, String userRole, String action, String details, String ipAddress) {
        this.userId = userId;
        this.userName = userName;
        this.userRole = userRole;
        this.action = action;
        this.details = details;
        this.ipAddress = ipAddress;
    }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public String getUserName() { return userName; }
    public void setUserName(String userName) { this.userName = userName; }
    public String getUserRole() { return userRole; }
    public void setUserRole(String userRole) { this.userRole = userRole; }
    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }
    public String getDetails() { return details; }
    public void setDetails(String details) { this.details = details; }
    public String getIpAddress() { return ipAddress; }
    public void setIpAddress(String ipAddress) { this.ipAddress = ipAddress; }
}
