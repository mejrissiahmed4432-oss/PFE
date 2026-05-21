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

        notificationService.createNotification(
            "New Equipment Request",
            "A new equipment request from " + saved.getCreatedByName() + " is pending your approval.",
            "INFO", "PROCUREMENT", saved.getId(), null, "IT_MANAGER"
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

    /**
     * Update an existing request.
     * Stock Manager: only allowed while PENDING_IT_APPROVAL and they are the owner.
     * IT Manager (isItManager=true): allowed at any status, any request.
     */
    public EquipmentRequest updateRequest(String id, EquipmentRequest updated, String requesterId, boolean isItManager) {
        return repository.findById(id).map(req -> {
            if (!isItManager) {
                if (req.getCreatedByUserId() != null && !req.getCreatedByUserId().equals(requesterId)) {
                    throw new SecurityException("You can only edit your own requests.");
                }
                if (req.getStatus() != RequestStatus.PENDING_IT_APPROVAL) {
                    throw new IllegalStateException("Request can only be edited while pending approval.");
                }
            }
            if (updated.getItems() != null) req.setItems(updated.getItems());
            if (updated.getNotes() != null) req.setNotes(updated.getNotes());
            req.setUpdatedAt(LocalDateTime.now());
            EquipmentRequest saved = repository.save(req);

            if (!isItManager) {
                notificationService.createNotification(
                    "Request Updated",
                    saved.getCreatedByName() + " updated their pending equipment request.",
                    "INFO", "PROCUREMENT", saved.getId(), null, "IT_MANAGER"
                );
            }
            return saved;
        }).orElseThrow(() -> new RuntimeException("Equipment request not found: " + id));
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

            notificationService.createNotification(
                "✅ Request Approved",
                "Your equipment request has been approved by IT and is ready for procurement.",
                "SUCCESS", "PROCUREMENT", saved.getId(), saved.getCreatedByUserId(), "STOCK_MANAGER"
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

            notificationService.createNotification(
                "❌ Request Rejected",
                "Your equipment request was rejected. Reason: " + reason,
                "ERROR", "PROCUREMENT", saved.getId(), saved.getCreatedByUserId(), null
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

            notificationService.createNotification(
                "📤 RFQ Sent to Suppliers",
                "Your approved equipment request has been sent to suppliers for quotation.",
                "INFO", "PROCUREMENT", req.getId(), req.getCreatedByUserId(), "STOCK_MANAGER"
            );
        });
    }

    /** Update status to RESPONDED when a supplier uploads a response */
    public void markAsResponded(String id) {
        repository.findById(id).ifPresent(req -> {
            if (req.getStatus() == RequestStatus.SENT_TO_SUPPLIERS) {
                req.setStatus(RequestStatus.RESPONDED);
                req.setUpdatedAt(LocalDateTime.now());
                repository.save(req);

                notificationService.createNotification(
                    "📩 Supplier Quotation Received",
                    "A supplier responded to the request from " + req.getCreatedByName() + ". Review quotations now.",
                    "INFO", "PROCUREMENT", req.getId(), null, "IT_MANAGER"
                );

                notificationService.createNotification(
                    "📩 Quotation Received",
                    "A supplier has submitted a quotation for your equipment request.",
                    "SUCCESS", "PROCUREMENT", req.getId(), req.getCreatedByUserId(), "STOCK_MANAGER"
                );
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

            notificationService.createNotification(
                "🛒 Order Confirmed",
                "A purchase order was confirmed with " + supplierName + " for your equipment request.",
                "SUCCESS", "PROCUREMENT", req.getId(), req.getCreatedByUserId(), "STOCK_MANAGER"
            );
        });
    }

    public void markAsReceived(String id) {
        repository.findById(id).ifPresent(req -> {
            req.setStatus(RequestStatus.RECEIVED);
            req.setUpdatedAt(LocalDateTime.now());
            repository.save(req);

            notificationService.createNotification(
                "📦 Items Received",
                "Equipment items from " + req.getCreatedByName() + "'s request have been received and added to inventory.",
                "SUCCESS", "PROCUREMENT", req.getId(), null, "IT_MANAGER"
            );
        });
    }
}
