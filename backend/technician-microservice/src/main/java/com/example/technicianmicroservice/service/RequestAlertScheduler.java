package com.example.technicianmicroservice.service;

import com.example.technicianmicroservice.model.PartRequest;
import com.example.technicianmicroservice.repository.PartRequestRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class RequestAlertScheduler {

    @Autowired
    private PartRequestRepository partRequestRepository;

    @Autowired
    private AlertService alertService;

    // Run every hour
    @Scheduled(cron = "0 0 * * * *")
    public void checkPendingRequests() {
        System.out.println("[RequestAlertScheduler] Checking for pending requests...");
        List<PartRequest> allRequests = partRequestRepository.findAll();

        for (PartRequest request : allRequests) {
            String key = "REQUEST_PENDING_" + request.getId();

            if ("PENDING".equalsIgnoreCase(request.getStatus())) {
                // Trigger alert
                String title = "Pending Part Request";
                String message = "Part request from " + request.getRequesterName() + " is pending review. Priority: " + request.getPriority() + ".";
                
                alertService.triggerSystemAlert(
                    key,
                    "REQUEST_PENDING",
                    "HIGH".equalsIgnoreCase(request.getPriority()) ? "HIGH" : "MEDIUM",
                    "ROLE",
                    "STOCK_MANAGER",
                    title,
                    message
                );
            } else {
                // Resolve alert if request is no longer pending (e.g. APPROVED or REJECTED)
                alertService.resolveSystemAlert(key);
            }
        }
    }
}
