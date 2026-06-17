package com.example.stockmanagermicroservice.procurement.controller;

import com.example.stockmanagermicroservice.procurement.model.RFQ;
import com.example.stockmanagermicroservice.procurement.service.RFQService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.example.stockmanagermicroservice.config.BlockchainTraceable;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/procurement/rfq")
@CrossOrigin(origins = "*")
public class RFQController {

    @Autowired
    private RFQService rfqService;

    /** POST /api/procurement/rfq — Create RFQ, generate PDF, send emails */
    @BlockchainTraceable(action = "Create RFQ")
    @PostMapping
    public ResponseEntity<?> createRFQ(@RequestBody Map<String, Object> body) {
        try {
            String requestId = (String) body.get("requestId");
            @SuppressWarnings("unchecked")
            List<String> supplierIds = (List<String>) body.get("supplierIds");
            @SuppressWarnings("unchecked")
            List<String> supplierEmails = (List<String>) body.get("supplierEmails");
            @SuppressWarnings("unchecked")
            List<Integer> selectedItemIndices = (List<Integer>) body.get("selectedItemIndices");

            RFQ rfq = rfqService.createAndSendRFQ(requestId, supplierIds, supplierEmails, selectedItemIndices);
            return ResponseEntity.ok(rfq);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    /** GET /api/procurement/rfq — List all RFQs */
    @GetMapping
    public ResponseEntity<List<RFQ>> getAllRFQs() {
        return ResponseEntity.ok(rfqService.getAllRFQs());
    }

    /** GET /api/procurement/rfq/{id} — Get RFQ by ID */
    @GetMapping("/{id}")
    public ResponseEntity<RFQ> getById(@PathVariable String id) {
        return rfqService.getRFQById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /** GET /api/procurement/rfq/request/{requestId} — Get RFQ by request ID */
    @GetMapping("/request/{requestId}")
    public ResponseEntity<RFQ> getByRequestId(@PathVariable String requestId) {
        return rfqService.getRFQByRequestId(requestId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /** POST /api/procurement/rfq/{id}/resend — Resend RFQ to selected suppliers */
    @BlockchainTraceable(action = "Resend RFQ")
    @PostMapping("/{id}/resend")
    public ResponseEntity<?> resendRFQ(@PathVariable String id, @RequestBody Map<String, Object> body) {
        try {
            @SuppressWarnings("unchecked")
            List<String> supplierIds = (List<String>) body.get("supplierIds");
            if (supplierIds == null || supplierIds.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "No suppliers selected for resend"));
            }
            RFQ rfq = rfqService.resendFailedRfq(id, supplierIds);
            return ResponseEntity.ok(rfq);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }
}
