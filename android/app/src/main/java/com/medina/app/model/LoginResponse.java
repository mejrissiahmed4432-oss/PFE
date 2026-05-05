package com.medina.app.model;

public class LoginResponse {
    private String firstName;
    private String lastName;
    private String email;
    private String role;
    private String photo;
    private String token;

    public String getFirstName() { return firstName; }
    public String getLastName() { return lastName; }
    public String getEmail() { return email; }
    public String getRole() { return role; }
    public String getPhoto() { return photo; }
    public String getToken() { return token; }
}
