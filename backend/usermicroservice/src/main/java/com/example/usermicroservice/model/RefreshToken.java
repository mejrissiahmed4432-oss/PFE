package com.example.usermicroservice.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Document(collection = "refresh_tokens")
public class RefreshToken {

    @Id
    private String id;
    private String token;
    private String userEmail;
    private Instant expiryDate;

    public RefreshToken() {}

    public RefreshToken(String token, String userEmail, Instant expiryDate) {
        this.token = token;
        this.userEmail = userEmail;
        this.expiryDate = expiryDate;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }

    public String getUserEmail() { return userEmail; }
    public void setUserEmail(String userEmail) { this.userEmail = userEmail; }

    public Instant getExpiryDate() { return expiryDate; }
    public void setExpiryDate(Instant expiryDate) { this.expiryDate = expiryDate; }
}
