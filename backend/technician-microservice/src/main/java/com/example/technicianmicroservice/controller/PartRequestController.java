package com.example.technicianmicroservice.controller;

import com.example.technicianmicroservice.model.PartRequest;
import com.example.technicianmicroservice.service.PartRequestService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/part-requests")
public class PartRequestController {

    @Autowired
    private PartRequestService service;

    @PostMapping
    public PartRequest createRequest(@RequestBody PartRequest request) {
        return service.createRequest(request);
    }

    @GetMapping("/my/{requesterId}")
    public List<PartRequest> getMyRequests(@PathVariable String requesterId) {
        return service.getMyRequests(requesterId);
    }

    @GetMapping
    public List<PartRequest> getAllRequests() {
        return service.getAllRequests();
    }

    @PutMapping("/{id}/status")
    public PartRequest updateStatus(@PathVariable String id, @RequestParam String status) {
        return service.updateStatus(id, status);
    }

    @PutMapping("/{id}")
    public PartRequest updateRequest(@PathVariable String id, @RequestBody PartRequest updateDetails) {
        return service.updateRequest(id, updateDetails);
    }

    @DeleteMapping("/{id}")
    public void deleteRequest(@PathVariable String id) {
        service.deleteRequest(id);
    }
}
