package com.example.technicianmicroservice.service;

import com.example.technicianmicroservice.model.PartRequest;
import com.example.technicianmicroservice.repository.PartRequestRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import jakarta.annotation.PostConstruct;

import java.util.List;

@Service
public class PartRequestService {

    @Autowired
    private PartRequestRepository repository;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private AlertService alertService;

    @Autowired
    private com.example.technicianmicroservice.client.StockManagerClient stockManagerClient;

    @org.springframework.context.event.EventListener(org.springframework.boot.context.event.ApplicationReadyEvent.class)
    public void init() {
        // System.out.println("Starting cleanup for technician Moetez...");
        // deleteRequestsByRequester("moetez");
        // System.out.println("Cleanup completed.");
    }

    private void deleteRequestsByRequester(String requesterId) {
        List<PartRequest> requests = repository.findByRequesterId(requesterId);
        repository.deleteAll(requests);
        
        List<PartRequest> requestsEmail = repository.findByRequesterId("moetez@gmail.com");
        repository.deleteAll(requestsEmail);

        List<PartRequest> requestsId = repository.findByRequesterId("69d5170dfd668941de3716b3");
        repository.deleteAll(requestsId);
    }
    public PartRequest createRequest(PartRequest request) {
        request.setStatus("PENDING");
        PartRequest saved = repository.save(request);
        
        // Notify Stock Managers
        notificationService.createNotification(
            "New Request",
            "A new part request with " + (saved.getItems() != null ? saved.getItems().size() : 0) + " items has been submitted. Priority: " + saved.getPriority(),
            "INFO", "PART_REQUEST", saved.getId(), null, "STOCK_MANAGER"
        );
        
        alertService.triggerSystemAlert(
            "REQUEST_PENDING_" + saved.getId(),
            "REQUEST_PENDING",
            "HIGH".equalsIgnoreCase(saved.getPriority()) ? "HIGH" : "MEDIUM",
            "ROLE",
            "STOCK_MANAGER",
            "Pending Part Request",
            "Part request from " + saved.getRequesterName() + " is pending review. Priority: " + saved.getPriority() + "."
        );
        
        // We no longer notify the requester upon submission per user request
        
        return saved;
    }

    public List<PartRequest> getMyRequests(String requesterId) {
        return repository.findByRequesterId(requesterId);
    }

    public List<PartRequest> getAllRequests() {
        return repository.findAll();
    }

    public PartRequest updateStatus(String requestId, String status) {
        PartRequest request = repository.findById(requestId).orElseThrow(() -> new RuntimeException("Request not found"));
        String oldStatus = request.getStatus();
        
        // --- 1. Feign Call BEFORE Database Save ---
        // If stock-manager is DOWN, Circuit Breaker throws an exception here and blocks execution,
        // so the database is never updated and the status remains PENDING.
        if ("APPROVED".equals(status) && !status.equals(oldStatus)) {
            java.util.Map<String, Object> allocateRequest = new java.util.HashMap<>();
            allocateRequest.put("requesterId", request.getRequesterId());
            allocateRequest.put("requesterName", request.getRequesterName());
            
            List<java.util.Map<String, Object>> parts = new java.util.ArrayList<>();
            if (request.getItems() != null) {
                for (com.example.technicianmicroservice.model.PartRequestItem item : request.getItems()) {
                    java.util.Map<String, Object> part = new java.util.HashMap<>();
                    part.put("name", item.getPartName());
                    part.put("specification", item.getSpecification());
                    part.put("qty", item.getQuantity());
                    parts.add(part);
                }
            }
            allocateRequest.put("parts", parts);
            stockManagerClient.allocateParts(allocateRequest);
            System.out.println("Forwarded allocation to Stock Manager for requester: " + request.getRequesterId());
        }

        // --- 2. Database Save ---
        request.setStatus(status);
        PartRequest saved = repository.save(request);
        
        if (!"PENDING".equalsIgnoreCase(status) && "PENDING".equalsIgnoreCase(oldStatus)) {
            alertService.resolveSystemAlert("REQUEST_PENDING_" + requestId);
        }

        // Notify Technician (Requester) if status changed
        if (!status.equals(oldStatus)) {
            String type = "INFO";
            String emoji = "ℹ️";
            if ("APPROVED".equals(status)) { type = "SUCCESS"; emoji = "✅"; }
            if ("REJECTED".equals(status)) { type = "INFO"; emoji = "❌"; }
            if ("COMPLETED".equals(status)) { type = "SUCCESS"; emoji = "✨"; }
            
            String title = "Part Request Update " + emoji;
            String msg = "Ticket related request status changed to: " + status + ". Please check your workbench.";

            if ("APPROVED".equals(status)) {
                title = "Request Accepted ✅";
                msg = "Your part request has been approved. You can now use the parts in your workbench.";
            } else if ("REJECTED".equals(status)) {
                title = "Request Refused ❌";
                msg = "Your part request has been refused by the stock manager.";
            }

            notificationService.createNotification(
                title,
                msg,
                type, "PART_REQUEST", saved.getId(), saved.getRequesterId(), null
            );
        }
        
        return saved;
    }

    public PartRequest updateRequest(String id, PartRequest updateDetails) {
        PartRequest request = repository.findById(id).orElseThrow(() -> new RuntimeException("Request not found"));
        if (!"PENDING".equals(request.getStatus()) && !"APPROVED".equals(request.getStatus())) {
            throw new RuntimeException("Cannot update a request that is not PENDING or APPROVED");
        }
        if (updateDetails.getItems() != null) request.setItems(updateDetails.getItems());
        if (updateDetails.getPriority() != null) request.setPriority(updateDetails.getPriority());
        if (updateDetails.getDescription() != null) request.setDescription(updateDetails.getDescription());
        
        PartRequest saved = repository.save(request);
        
        // Removed 'Request Updated' notification per user request
        
        return saved;
    }

    public void deleteRequest(String id) {
        PartRequest request = repository.findById(id).orElseThrow(() -> new RuntimeException("Request not found"));
        if (!"PENDING".equals(request.getStatus())) {
            throw new RuntimeException("Cannot cancel a request that is not PENDING");
        }
        repository.delete(request);
        alertService.resolveSystemAlert("REQUEST_PENDING_" + id);
        
        // Removed 'Request Cancelled' notification per user request
    }

    public void consumeParts(String requesterId, List<com.example.technicianmicroservice.controller.PartRequestController.PartConsumeRequest> consumedParts) {
        if (consumedParts == null || consumedParts.isEmpty()) return;
        List<PartRequest> approvedRequests = repository.findByRequesterId(requesterId).stream()
                .filter(r -> "APPROVED".equals(r.getStatus()))
                .collect(java.util.stream.Collectors.toList());
                
        for (com.example.technicianmicroservice.controller.PartRequestController.PartConsumeRequest consumed : consumedParts) {
            int remainingToConsume = consumed.qty;
            for (PartRequest req : approvedRequests) {
                if (remainingToConsume <= 0) break;
                if (req.getItems() != null) {
                    for (com.example.technicianmicroservice.model.PartRequestItem item : req.getItems()) {
                        if (item.getPartName() != null && item.getPartName().equals(consumed.name) && 
                            (item.getSpecification() == null || item.getSpecification().equals(consumed.specification))) {
                            int available = item.getQuantity() != null ? item.getQuantity() : 0;
                            if (available > 0) {
                                int deduct = Math.min(available, remainingToConsume);
                                item.setQuantity(available - deduct);
                                remainingToConsume -= deduct;
                            }
                        }
                    }
                }
                repository.save(req);
            }
        }
        
        // ── Forward to Stock Manager for physical inventory update ──
        stockManagerClient.consumeParts(requesterId, consumedParts);
        System.out.println("Forwarded consumption to Stock Manager: " + consumedParts.size() + " items for requester " + requesterId);
    }
}