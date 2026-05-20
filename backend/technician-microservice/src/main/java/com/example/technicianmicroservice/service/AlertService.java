package com.example.technicianmicroservice.service;

import com.example.technicianmicroservice.client.UserClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class AlertService {

    @Autowired
    private UserClient userClient;

    public void triggerSystemAlert(String key, String type, String priority, String targetType, String targetId, String title, String message) {
        Map<String, String> body = new HashMap<>();
        body.put("key", key);
        body.put("type", type);
        body.put("priority", priority);
        body.put("targetType", targetType);
        if (targetId != null) body.put("targetId", targetId);
        body.put("title", title);
        body.put("message", message);

        try {
            userClient.triggerSystemAlert(body);
        } catch (Exception e) {
            System.err.println("Failed to send system alert to user-microservice: " + e.getMessage());
        }
    }

    public void resolveSystemAlert(String key) {
        try {
            userClient.resolveSystemAlert(key);
        } catch (Exception e) {
            System.err.println("Failed to resolve system alert in user-microservice: " + e.getMessage());
        }
    }
}
