package com.example.stockmanagermicroservice.procurement.controller;

import com.example.stockmanagermicroservice.procurement.model.EquipmentRequest;
import com.example.stockmanagermicroservice.procurement.model.RFQToken;
import com.example.stockmanagermicroservice.procurement.model.SupplierResponse;
import com.example.stockmanagermicroservice.procurement.repository.RFQTokenRepository;
import com.example.stockmanagermicroservice.procurement.service.EquipmentRequestService;
import com.example.stockmanagermicroservice.procurement.service.SupplierResponseService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/public/supplier-response")
@CrossOrigin(origins = "*")
public class PublicSupplierController {

    @Autowired
    private RFQTokenRepository tokenRepository;

    @Autowired
    private EquipmentRequestService requestService;

    @Autowired
    private SupplierResponseService responseService;

    /**
     * GET /api/public/supplier-response/token/{token}
     * Validates the token and returns the RFQ details so the supplier can view them.
     */
    @GetMapping("/token/{tokenStr}")
    public ResponseEntity<?> getRequestByToken(@PathVariable String tokenStr) {
        Optional<RFQToken> tokenOpt = tokenRepository.findByToken(tokenStr);
        if (tokenOpt.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid token"));
        }

        RFQToken token = tokenOpt.get();
        if (token.getExpiresAt().isBefore(LocalDateTime.now())) {
            return ResponseEntity.badRequest().body(Map.of("error", "Token expired"));
        }
        if (token.isUsed()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Token has already been used"));
        }

        EquipmentRequest request = requestService.getRequestById(token.getRequestId())
                .orElse(null);
                
        if (request == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "Associated request not found"));
        }

        Map<String, Object> response = new HashMap<>();
        response.put("token", token);
        response.put("request", request);

        return ResponseEntity.ok(response);
    }

    /**
     * POST /api/public/supplier-response/submit
     * Submits a quotation using a valid token.
     */
    @PostMapping("/submit")
    public ResponseEntity<?> submitResponse(
            @RequestParam("token") String tokenStr,
            @RequestParam(value = "totalPrice", required = false) Double totalPrice,
            @RequestParam(value = "deliveryDays", required = false) Integer deliveryDays,
            @RequestParam(value = "notes", required = false) String notes,
            @RequestParam("file") MultipartFile file) {
            
        try {
            Optional<RFQToken> tokenOpt = tokenRepository.findByToken(tokenStr);
            if (tokenOpt.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Invalid token"));
            }

            RFQToken token = tokenOpt.get();
            if (token.getExpiresAt().isBefore(LocalDateTime.now())) {
                return ResponseEntity.badRequest().body(Map.of("error", "Token expired"));
            }
            if (token.isUsed()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Token has already been used"));
            }

            // Securely lookup IDs from the token
            String rfqId = token.getRfqId();
            String requestId = token.getRequestId();
            String supplierId = token.getSupplierId();
            String supplierName = token.getSupplierName();

            SupplierResponse response = responseService.uploadResponse(
                    rfqId, requestId, supplierId, supplierName,
                    totalPrice, deliveryDays, notes, "TND", file);

            // Mark token as used
            token.setUsed(true);
            tokenRepository.save(token);

            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }
}
