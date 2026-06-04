package com.medina.app.model;

public class User {
    private String id;
    private String firstName;
    private String lastName;
    private String email;
    private String role;
    private String phoneNumber;
    private String department;
    private String joinedDate;
    private boolean online;

    // Transient fields populated from ConversationSummary (not from server User JSON)
    private transient String lastMessage;
    private transient String lastMessageTime;
    private transient int unreadCount;

    public User() {}

    public User(String id, String firstName, String lastName, String email, String role, String phoneNumber, String department, String joinedDate) {
        this.id = id;
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.role = role;
        this.phoneNumber = phoneNumber;
        this.department = department;
        this.joinedDate = joinedDate;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }

    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public String getPhoneNumber() { return phoneNumber; }
    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }

    public String getDepartment() { return department; }
    public void setDepartment(String department) { this.department = department; }

    public String getJoinedDate() { return joinedDate; }
    public void setJoinedDate(String joinedDate) { this.joinedDate = joinedDate; }

    public boolean isOnline() { return online; }
    public void setOnline(boolean online) { this.online = online; }

    public String getLastMessage() { return lastMessage; }
    public void setLastMessage(String lastMessage) { this.lastMessage = lastMessage; }

    public String getLastMessageTime() { return lastMessageTime; }
    public void setLastMessageTime(String lastMessageTime) { this.lastMessageTime = lastMessageTime; }

    public int getUnreadCount() { return unreadCount; }
    public void setUnreadCount(int unreadCount) { this.unreadCount = unreadCount; }

    /** Convenience: returns "firstName lastName" or just one of them, never null */
    public String getName() {
        String fn = firstName != null ? firstName.trim() : "";
        String ln = lastName  != null ? lastName.trim()  : "";
        if (!fn.isEmpty() && !ln.isEmpty()) return fn + " " + ln;
        if (!fn.isEmpty()) return fn;
        if (!ln.isEmpty()) return ln;
        return email != null ? email : "";
    }

    /** Allows setting a synthetic display name (used for fallback objects) */
    public void setName(String fullName) {
        if (fullName == null) return;
        String[] parts = fullName.trim().split(" ", 2);
        this.firstName = parts[0];
        this.lastName  = parts.length > 1 ? parts[1] : "";
    }
}
