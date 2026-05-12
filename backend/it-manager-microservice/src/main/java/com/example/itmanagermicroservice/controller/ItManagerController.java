package com.example.itmanagermicroservice.controller;

import com.example.itmanagermicroservice.dto.EmployeeDTO;
import com.example.itmanagermicroservice.service.ItManagerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/it-manager")
public class ItManagerController {

    @Autowired
    private ItManagerService itManagerService;

    @GetMapping("/employees")
    public List<EmployeeDTO> getAllEmployees() {
        return itManagerService.getAllEmployees();
    }

    @GetMapping("/users")
    public List<Map<String, Object>> getAllUsers() {
        return itManagerService.getAllUsers();
    }

    @PostMapping("/provision")
    public Map<String, Object> provisionUser(@RequestBody Map<String, Object> request) {
        return itManagerService.provisionUser(request);
    }

    @PutMapping("/users/{id}/status")
    public Map<String, Object> updateUserStatus(@PathVariable String id, @RequestBody Map<String, String> request) {
        return itManagerService.updateUserStatus(id, request);
    }

    @PutMapping("/users/{id}/role")
    public Map<String, Object> updateUserRole(@PathVariable String id, @RequestBody Map<String, String> request) {
        return itManagerService.updateUserRole(id, request);
    }

    @PostMapping("/users/{id}/resend-invitation")
    public Map<String, Object> resendInvitation(@PathVariable String id) {
        return itManagerService.resendInvitation(id);
    }

    @DeleteMapping("/users/{id}")
    public ResponseEntity<?> deleteUser(@PathVariable String id) {
        itManagerService.deleteUser(id);
        return ResponseEntity.ok().build();
    }
}
