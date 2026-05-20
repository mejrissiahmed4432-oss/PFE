package com.example.technicianmicroservice.service;

import com.example.technicianmicroservice.client.UserClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class NotificationService {

    @Autowired
    private UserClient userClient;

    public void createNotification(String title, String message, String type, String category, String relatedId, String recipientId, String targetRole) {
        Map<String, String> body = new HashMap<>();
        body.put("title", title);
        body.put("message", message);
        body.put("type", type);
        body.put("category", category);
        body.put("relatedId", relatedId);
        body.put("recipientId", recipientId);
        body.put("targetRole", targetRole);

        try {
            userClient.createNotification(body);
        } catch (Exception e) {
            System.err.println("Failed to send notification via Feign: " + e.getMessage());
        }
    }
}
