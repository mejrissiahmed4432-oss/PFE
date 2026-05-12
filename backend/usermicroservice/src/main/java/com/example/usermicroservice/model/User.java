package com.example.usermicroservice.model;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "users")
public class User {
    @Id
    private String id;
    private String firstName;
    private String lastName;

    @org.springframework.data.mongodb.core.index.Indexed(unique = true)
    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    private String email;

    private String password;

    private String photo;
    private Role role;
    
    private String phoneNumber;
    private String employeeId;
    
    private String resetToken;
    private java.time.LocalDateTime resetTokenExpiry;
    
    @org.springframework.data.annotation.Transient
    private boolean online;
    
    private java.time.LocalDateTime lastActive;
    private String status = "PENDING";
    private java.time.LocalDateTime lastLogin;
    private java.time.LocalDateTime createdAt = java.time.LocalDateTime.now();

    public User() {
        // Default photo if none provided
        this.photo = "default-user.png";
    }

    public User(String firstName, String lastName, String email, String password, Role role) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.password = password;
        this.role = role;
        this.status = "PENDING"; // New users start as PENDING
        this.photo = "default-user.png";
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }

    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getPhoto() { return photo; }
    public void setPhoto(String photo) { this.photo = photo; }

    public Role getRole() { return role; }
    public void setRole(Role role) { this.role = role; }

    public String getPhoneNumber() { return phoneNumber; }
    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }

    public String getEmployeeId() { return employeeId; }
    public void setEmployeeId(String employeeId) { this.employeeId = employeeId; }

    public String getResetToken() { return resetToken; }
    public void setResetToken(String resetToken) { this.resetToken = resetToken; }

    public java.time.LocalDateTime getResetTokenExpiry() { return resetTokenExpiry; }
    public void setResetTokenExpiry(java.time.LocalDateTime resetTokenExpiry) { this.resetTokenExpiry = resetTokenExpiry; }

    public java.time.LocalDateTime getLastActive() { return lastActive; }
    public void setLastActive(java.time.LocalDateTime lastActive) { this.lastActive = lastActive; }

    public UserStatus getStatus() { 
        String s = this.status;
        if (s == null || s.trim().isEmpty()) return UserStatus.ACTIVE;
        try {
            return UserStatus.valueOf(s.toUpperCase());
        } catch (Exception e) {
            return UserStatus.ACTIVE;
        }
    }

    public void setStatus(UserStatus status) { 
        this.status = status != null ? status.name() : "PENDING"; 
    }

    public java.time.LocalDateTime getLastLogin() { return lastLogin; }
    public void setLastLogin(java.time.LocalDateTime lastLogin) { this.lastLogin = lastLogin; }

    public java.time.LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(java.time.LocalDateTime createdAt) { this.createdAt = createdAt; }

    public boolean isOnline() { return online; }
    public void setOnline(boolean online) { this.online = online; }
}
