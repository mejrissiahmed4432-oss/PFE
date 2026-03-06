package com.example.usermicroservice.dto;

import com.example.usermicroservice.model.Role;

public class LoginResponse {
    private String firstName;
    private String lastName;
    private String email;
    private Role role;
    private String photo;
    private String token;

    public LoginResponse(String firstName, String lastName, String email, Role role, String photo, String token) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.role = role;
        this.photo = photo;
        this.token = token;
    }

    // Getters and Setters
    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }

    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public Role getRole() { return role; }
    public void setRole(Role role) { this.role = role; }

    public String getPhoto() { return photo; }
    public void setPhoto(String photo) { this.photo = photo; }

    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }
}
