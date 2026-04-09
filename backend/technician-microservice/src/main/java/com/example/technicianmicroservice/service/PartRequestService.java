package com.example.technicianmicroservice.service;

import com.example.technicianmicroservice.model.PartRequest;
import com.example.technicianmicroservice.repository.PartRequestRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PartRequestService {

    @Autowired
    private PartRequestRepository repository;

    public PartRequest createRequest(PartRequest request) {
        request.setStatus("PENDING");
        return repository.save(request);
    }

    public List<PartRequest> getMyRequests(String requesterId) {
        return repository.findByRequesterId(requesterId);
    }

    public List<PartRequest> getAllRequests() {
        return repository.findAll();
    }

    public PartRequest updateStatus(String requestId, String status) {
        PartRequest request = repository.findById(requestId).orElseThrow(() -> new RuntimeException("Request not found"));
        request.setStatus(status);
        return repository.save(request);
    }

    public PartRequest updateRequest(String id, PartRequest updateDetails) {
        PartRequest request = repository.findById(id).orElseThrow(() -> new RuntimeException("Request not found"));
        if (!"PENDING".equals(request.getStatus())) {
            throw new RuntimeException("Cannot update a request that is not PENDING");
        }
        if (updateDetails.getQuantity() != null) request.setQuantity(updateDetails.getQuantity());
        if (updateDetails.getPriority() != null) request.setPriority(updateDetails.getPriority());
        if (updateDetails.getSpecification() != null) request.setSpecification(updateDetails.getSpecification());
        return repository.save(request);
    }

    public void deleteRequest(String id) {
        PartRequest request = repository.findById(id).orElseThrow(() -> new RuntimeException("Request not found"));
        if (!"PENDING".equals(request.getStatus())) {
            throw new RuntimeException("Cannot cancel a request that is not PENDING");
        }
        repository.delete(request);
    }
}
