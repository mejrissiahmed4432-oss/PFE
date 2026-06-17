package com.example.usermicroservice.service;

import com.example.usermicroservice.model.PersonalRequest;
import com.example.usermicroservice.repository.PersonalRequestRepository;
import com.example.usermicroservice.client.StockClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
public class PersonalRequestService {

    @Autowired
    private PersonalRequestRepository repository;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private StockClient stockClient;

    public PersonalRequest createRequest(PersonalRequest request) {
        request.setStatus("PENDING");
        request.setCreatedAt(LocalDateTime.now());
        request.setUpdatedAt(LocalDateTime.now());
        
        PersonalRequest saved = repository.save(request);
        
        // Notify IT Managers
        notificationService.createNotification(
            "New Personal Resource Request",
            request.getUserName() + " has requested new resources.",
            "INFO",
            "REQUEST",
            saved.getId(),
            null,
            "IT_MANAGER"
        );
        
        return saved;
    }

    public List<PersonalRequest> getRequestsByUser(String userId) {
        return repository.findByUserId(userId);
    }

    public List<PersonalRequest> getPendingRequests() {
        return repository.findByStatus("PENDING");
    }
    
    public List<PersonalRequest> getHistory() {
        List<PersonalRequest> approved = repository.findByStatus("APPROVED");
        List<PersonalRequest> rejected = repository.findByStatus("REJECTED");
        List<PersonalRequest> history = new java.util.ArrayList<>();
        history.addAll(approved);
        history.addAll(rejected);
        history.sort((a, b) -> {
            if (a.getReviewedAt() == null) return 1;
            if (b.getReviewedAt() == null) return -1;
            return b.getReviewedAt().compareTo(a.getReviewedAt());
        });
        return history;
    }
    
    public List<PersonalRequest> getAllRequests() {
        return repository.findAll();
    }

    public PersonalRequest getRequestById(String id) {
        return repository.findById(id).orElseThrow(() -> new RuntimeException("Request not found"));
    }

    public PersonalRequest approveRequest(String id, String note) {
        PersonalRequest req = getRequestById(id);
        if (!"PENDING".equals(req.getStatus())) {
            throw new RuntimeException("Request is not PENDING");
        }
        
        req.setStatus("APPROVED");
        req.setItManagerNote(note);
        req.setReviewedAt(LocalDateTime.now());
        req.setUpdatedAt(LocalDateTime.now());
        PersonalRequest saved = repository.save(req);
        
        // Notify User
        notificationService.createNotification(
            "Resource Request Approved",
            "Your resource request has been approved by the IT Manager.",
            "SUCCESS",
            "REQUEST",
            saved.getId(),
            req.getUserId(),
            null
        );
        
        return saved;
    }

    public PersonalRequest rejectRequest(String id, String note) {
        PersonalRequest req = getRequestById(id);
        if (!"PENDING".equals(req.getStatus())) {
            throw new RuntimeException("Request is not PENDING");
        }
        
        req.setStatus("REJECTED");
        req.setItManagerNote(note);
        req.setReviewedAt(LocalDateTime.now());
        req.setUpdatedAt(LocalDateTime.now());
        PersonalRequest saved = repository.save(req);
        
        // Notify User
        notificationService.createNotification(
            "Resource Request Rejected",
            "Your resource request was rejected by the IT Manager.",
            "WARNING",
            "REQUEST",
            saved.getId(),
            req.getUserId(),
            null
        );
        
        return saved;
    }

    public void deleteRequest(String id) {
        PersonalRequest req = getRequestById(id);
        if (!"PENDING".equals(req.getStatus())) {
            throw new RuntimeException("Only PENDING requests can be deleted");
        }
        repository.deleteById(id);
    }

    public List<Map<String, Object>> getAvailableEquipment() {
        return stockClient.getAvailableEquipment();
    }

    public List<Map<String, Object>> getAvailableSoftware() {
        return stockClient.getAvailableSoftware();
    }
}
