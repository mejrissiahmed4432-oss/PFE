package com.example.usermicroservice.controller;

import com.example.usermicroservice.model.PersonalRequest;
import com.example.usermicroservice.service.PersonalRequestService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/personal-requests")
@CrossOrigin(origins = "*")
public class PersonalRequestController {

    @Autowired
    private PersonalRequestService service;

    @PostMapping
    public ResponseEntity<PersonalRequest> createRequest(@RequestBody PersonalRequest request) {
        return ResponseEntity.ok(service.createRequest(request));
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<PersonalRequest>> getMyRequests(@PathVariable String userId) {
        return ResponseEntity.ok(service.getRequestsByUser(userId));
    }

    @GetMapping("/pending")
    public ResponseEntity<List<PersonalRequest>> getPendingRequests() {
        return ResponseEntity.ok(service.getPendingRequests());
    }

    @GetMapping("/history")
    public ResponseEntity<List<PersonalRequest>> getHistory() {
        return ResponseEntity.ok(service.getHistory());
    }

    @PutMapping("/{id}/approve")
    public ResponseEntity<PersonalRequest> approveRequest(@PathVariable String id, @RequestBody Map<String, String> payload) {
        String note = payload.get("note");
        return ResponseEntity.ok(service.approveRequest(id, note));
    }

    @PutMapping("/{id}/reject")
    public ResponseEntity<PersonalRequest> rejectRequest(@PathVariable String id, @RequestBody Map<String, String> payload) {
        String note = payload.get("note");
        return ResponseEntity.ok(service.rejectRequest(id, note));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteRequest(@PathVariable String id) {
        service.deleteRequest(id);
        return ResponseEntity.ok().build();
    }

    // Proxy endpoints to fetch available resources from Stock Manager
    @GetMapping("/available-equipment")
    public ResponseEntity<List<Map<String, Object>>> getAvailableEquipment() {
        return ResponseEntity.ok(service.getAvailableEquipment());
    }

    @GetMapping("/available-software")
    public ResponseEntity<List<Map<String, Object>>> getAvailableSoftware() {
        return ResponseEntity.ok(service.getAvailableSoftware());
    }
}
