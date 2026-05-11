package com.example.stockmanagermicroservice.procurement.controller;

import com.example.stockmanagermicroservice.procurement.model.EquipmentRequest;
import com.example.stockmanagermicroservice.procurement.service.EquipmentRequestService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/procurement/requests")
@CrossOrigin(origins = "*")
public class EquipmentRequestController {

    @Autowired
    private EquipmentRequestService service;

    /** POST /api/procurement/requests — Stock Manager creates request */
    @PostMapping
    public ResponseEntity<EquipmentRequest> createRequest(@RequestBody EquipmentRequest request) {
        return ResponseEntity.ok(service.createRequest(request));
    }

    /** GET /api/procurement/requests — Get all requests */
    @GetMapping
    public ResponseEntity<List<EquipmentRequest>> getAllRequests() {
        return ResponseEntity.ok(service.getAllRequests());
    }

    /** GET /api/procurement/requests/user/{userId} — Get requests by user */
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<EquipmentRequest>> getByUser(@PathVariable String userId) {
        return ResponseEntity.ok(service.getRequestsByUser(userId));
    }

    /** GET /api/procurement/requests/{id} — Get request by ID */
    @GetMapping("/{id}")
    public ResponseEntity<EquipmentRequest> getById(@PathVariable String id) {
        return service.getRequestById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /** PUT /api/procurement/requests/{id}/approve — IT Manager approves */
    @PutMapping("/{id}/approve")
    public ResponseEntity<EquipmentRequest> approve(@PathVariable String id) {
        try {
            return ResponseEntity.ok(service.approveRequest(id));
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /** PUT /api/procurement/requests/{id}/reject — IT Manager rejects */
    @PutMapping("/{id}/reject")
    public ResponseEntity<EquipmentRequest> reject(
            @PathVariable String id,
            @RequestBody(required = false) Map<String, String> body) {
        try {
            String reason = body != null ? body.getOrDefault("reason", "") : "";
            return ResponseEntity.ok(service.rejectRequest(id, reason));
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().build();
        }
    }
}
