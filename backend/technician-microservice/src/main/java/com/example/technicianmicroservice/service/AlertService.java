package com.example.technicianmicroservice.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Service
public class AlertService {

    @Autowired
    private RestTemplate restTemplate;

    private static final String USER_SERVICE_SYSTEM_ALERT_URL = "http://user-microservice/api/alerts/system";
    private static final String USER_SERVICE_RESOLVE_ALERT_URL = "http://user-microservice/api/alerts/system/{key}/resolve";

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
            restTemplate.postForEntity(USER_SERVICE_SYSTEM_ALERT_URL, body, Void.class);
        } catch (Exception e) {
            System.err.println("Failed to send system alert to user-microservice: " + e.getMessage());
        }
    }

    public void resolveSystemAlert(String key) {
        try {
            restTemplate.postForEntity(USER_SERVICE_RESOLVE_ALERT_URL, null, Void.class, key);
        } catch (Exception e) {
            System.err.println("Failed to resolve system alert in user-microservice: " + e.getMessage());
        }
    }
}
