package com.example.technicianmicroservice.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.Map;

@FeignClient(name = "user-microservice", fallback = UserClientFallback.class)
public interface UserClient {

    @PostMapping("/api/notifications")
    void createNotification(@RequestBody Map<String, String> body);

    @PostMapping("/api/alerts/system")
    void triggerSystemAlert(@RequestBody Map<String, String> body);

    @PostMapping("/api/alerts/system/{key}/resolve")
    void resolveSystemAlert(@PathVariable("key") String key);
}
