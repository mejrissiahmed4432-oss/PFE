package com.example.usermicroservice.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import java.util.List;
import java.util.Map;

@FeignClient(name = "stock-manager-microservice", path = "/api", fallback = StockClientFallback.class)
public interface StockClient {
    
    @GetMapping("/equipment?status=Available")
    List<Map<String, Object>> getAvailableEquipment();

    @GetMapping("/software/available")
    List<Map<String, Object>> getAvailableSoftware();
}
