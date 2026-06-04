package com.example.technicianmicroservice.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;
import java.util.Map;

@FeignClient(name = "stock-manager-microservice", fallback = StockManagerClientFallback.class)
public interface StockManagerClient {

    @PostMapping("/api/equipment/allocate-parts")
    void allocateParts(@RequestBody Map<String, Object> requestedParts);

    @PostMapping("/api/equipment/consume-parts/{requesterId}")
    void consumeParts(@PathVariable("requesterId") String requesterId, @RequestBody Object consumedParts);
}
