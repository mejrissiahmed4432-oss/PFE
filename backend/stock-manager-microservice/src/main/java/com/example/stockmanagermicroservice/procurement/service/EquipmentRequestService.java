package com.example.stockmanagermicroservice.procurement.service;

import com.example.stockmanagermicroservice.procurement.model.EquipmentRequest;
import com.example.stockmanagermicroservice.procurement.model.RequestStatus;
import com.example.stockmanagermicroservice.procurement.repository.EquipmentRequestRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.example.stockmanagermicroservice.service.NotificationService;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class EquipmentRequestService {

    @Autowired
    private EquipmentRequestRepository repository;

    @Autowired
    private NotificationService notificationService;

    /** Stock Manager creates a new equipment request */
    public EquipmentRequest createRequest(EquipmentRequest request) {
        request.setStatus(RequestStatus.PENDING_IT_APPROVAL);
        request.setCreatedAt(LocalDateTime.now());
        request.setUpdatedAt(LocalDateTime.now());
        EquipmentRequest saved = repository.save(request);

        // Notify IT Managers
        notificationService.createNotification(
            "New Equipment Request",
            "A new equipment request from " + saved.getCreatedByName() + " is pending approval.",
            "INFO",
            "PROCUREMENT",
            saved.getId(),
            null,
            "IT_MANAGER"
        );

        return saved;
    }

    /** Get all requests ordered by most recent */
    public List<EquipmentRequest> getAllRequests() {
        return repository.findAllByOrderByCreatedAtDesc();
    }

    /** Get requests created by a specific user (Stock Manager view) */
    public List<EquipmentRequest> getRequestsByUser(String userId) {
        return repository.findByCreatedByUserId(userId);
    }

    /** Get a single request by ID */
    public Optional<EquipmentRequest> getRequestById(String id) {
        return repository.findById(id);
    }

    /** IT Manager approves a request */
    public EquipmentRequest approveRequest(String id) {
        return repository.findById(id).map(req -> {
            if (req.getStatus() != RequestStatus.PENDING_IT_APPROVAL) {
                throw new IllegalStateException("Request is not pending approval. Current status: " + req.getStatus());
            }
            req.setStatus(RequestStatus.APPROVED);
            req.setUpdatedAt(LocalDateTime.now());
            EquipmentRequest saved = repository.save(req);

            // Notify Creator
            notificationService.createNotification(
                "Request Approved",
                "Your equipment request has been approved by IT.",
                "SUCCESS",
                "PROCUREMENT",
                saved.getId(),
                saved.getCreatedByUserId(),
                "STOCK_MANAGER"
            );

            return saved;
        }).orElseThrow(() -> new RuntimeException("Equipment request not found: " + id));
    }

    /** IT Manager rejects a request */
    public EquipmentRequest rejectRequest(String id, String reason) {
        return repository.findById(id).map(req -> {
            if (req.getStatus() != RequestStatus.PENDING_IT_APPROVAL) {
                throw new IllegalStateException("Request is not pending approval. Current status: " + req.getStatus());
            }
            req.setStatus(RequestStatus.REJECTED);
            req.setRejectionReason(reason);
            req.setUpdatedAt(LocalDateTime.now());
            EquipmentRequest saved = repository.save(req);

            // Notify Creator
            notificationService.createNotification(
                "Request Rejected",
                "Your equipment request has been rejected. Reason: " + reason,
                "ERROR",
                "PROCUREMENT",
                saved.getId(),
                saved.getCreatedByUserId(),
                null
            );

            return saved;
        }).orElseThrow(() -> new RuntimeException("Equipment request not found: " + id));
    }

    /** Update status to SENT_TO_SUPPLIERS after RFQ is created */
    public void markAsSentToSuppliers(String id) {
        repository.findById(id).ifPresent(req -> {
            req.setStatus(RequestStatus.SENT_TO_SUPPLIERS);
            req.setUpdatedAt(LocalDateTime.now());
            repository.save(req);
        });
    }

    /** Update status to RESPONDED when a supplier uploads a response */
    public void markAsResponded(String id) {
        repository.findById(id).ifPresent(req -> {
            if (req.getStatus() == RequestStatus.SENT_TO_SUPPLIERS) {
                req.setStatus(RequestStatus.RESPONDED);
                req.setUpdatedAt(LocalDateTime.now());
                repository.save(req);
            }
        });
    }

    /** Update status to ORDER_CONFIRMED when purchase order is created */
    public void markAsOrderConfirmed(String id, String supplierName) {
        repository.findById(id).ifPresent(req -> {
            req.setStatus(RequestStatus.ORDER_CONFIRMED);
            req.setSupplierName(supplierName);
            req.setUpdatedAt(LocalDateTime.now());
            repository.save(req);
        });
    }

    public void markAsReceived(String id) {
        repository.findById(id).ifPresent(req -> {
            req.setStatus(RequestStatus.RECEIVED);
            req.setUpdatedAt(LocalDateTime.now());
            repository.save(req);
        });
    }
}
