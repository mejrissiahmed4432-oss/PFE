package com.example.stockmanagermicroservice.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.Map;

@FeignClient(name = "ai-service", url = "${app.ai-service.url:http://localhost:8085}", fallback = AiClientFallback.class)
public interface AiClient {

    @PostMapping("/api/ai/predict-maintenance")
    Map<String, Object> predictMaintenance(@RequestBody Map<String, Object> request);
}
