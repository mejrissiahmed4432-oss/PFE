package com.example.technicianmicroservice.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Service
public class NotificationService {

    @Autowired
    private RestTemplate restTemplate;

    private static final String USER_SERVICE_URL = "http://user-microservice/api/notifications";

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
            restTemplate.postForEntity(USER_SERVICE_URL, body, Void.class);
        } catch (Exception e) {
            System.err.println("Failed to send notification to user-microservice: " + e.getMessage());
        }
    }
}
