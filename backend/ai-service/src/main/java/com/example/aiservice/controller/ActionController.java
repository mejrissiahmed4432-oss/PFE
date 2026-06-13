package com.example.aiservice.controller;

import com.example.aiservice.config.BlockchainTraceable;
import com.example.aiservice.model.AiResponse;
import com.example.aiservice.service.ActionExecutorService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Controller for executing confirmed AI actions.
 */
@RestController
@RequestMapping("/ai/action")
@CrossOrigin(origins = "*")
public class ActionController {

    private static final Logger log = LoggerFactory.getLogger(ActionController.class);
    private final ActionExecutorService actionExecutor;

    public ActionController(ActionExecutorService actionExecutor) {
        this.actionExecutor = actionExecutor;
    }

    @BlockchainTraceable(action = "Execute AI action")
    @PostMapping("/execute")
    public ResponseEntity<AiResponse> executeAction(@RequestBody Map<String, Object> request) {
        String actionType = (String) request.get("actionType");
        Map<String, Object> payload = (Map<String, Object>) request.get("payload");
        String role = (String) request.get("role");
        String userId = (String) request.get("userId");

        log.info("Executing action: {} for user: {} with role: {}", actionType, userId, role);

        String resultMessage = actionExecutor.execute(actionType, payload, role, userId);
        
        AiResponse response = new AiResponse();
        response.setSuccess(resultMessage.contains("✅"));
        response.setAnswer(resultMessage);
        response.setRole(role);
        
        return ResponseEntity.ok(response);
    }
}
