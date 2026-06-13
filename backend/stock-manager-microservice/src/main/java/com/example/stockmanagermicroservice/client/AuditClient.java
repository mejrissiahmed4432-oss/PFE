package com.example.stockmanagermicroservice.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.Map;

@FeignClient(name = "employee-microservice", path = "/api/employees/audit", fallback = AuditClientFallback.class)
public interface AuditClient {

    @PostMapping("/log")
    Map<String, Object> logEvent(@RequestBody Map<String, Object> request);
}
