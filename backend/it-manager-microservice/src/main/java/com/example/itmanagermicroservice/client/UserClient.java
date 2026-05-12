package com.example.itmanagermicroservice.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@FeignClient(name = "user-microservice")
public interface UserClient {
    @GetMapping("/api/users")
    List<Map<String, Object>> getAllUsers();

    @PostMapping("/api/users/provision")
    Map<String, Object> provisionUser(@RequestBody Map<String, Object> request);

    @PutMapping("/api/users/{id}/status")
    Map<String, Object> updateUserStatus(@PathVariable("id") String id, @RequestBody Map<String, String> request);

    @PutMapping("/api/users/{id}/role")
    Map<String, Object> updateUserRole(@PathVariable("id") String id, @RequestBody Map<String, String> request);

    @PostMapping("/api/users/{id}/resend-invitation")
    Map<String, Object> resendInvitation(@PathVariable("id") String id);

    @DeleteMapping("/api/users/{id}")
    void deleteUser(@PathVariable("id") String id);
}
