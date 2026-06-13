package com.example.stockmanagermicroservice.procurement.controller;

import com.example.stockmanagermicroservice.config.BlockchainTraceable;
import com.example.stockmanagermicroservice.procurement.model.PurchaseOrder;
import com.example.stockmanagermicroservice.procurement.service.PurchaseOrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/procurement/orders")
@CrossOrigin(origins = "*")
public class PurchaseOrderController {

    @Autowired
    private PurchaseOrderService service;

    /** POST /api/procurement/orders — Create a Purchase Order */
    
    @PostMapping
    public ResponseEntity<?> createOrder(@RequestBody Map<String, String> body) {
        try {
            String requestId  = body.get("requestId");
            String rfqId      = body.get("rfqId");
            String responseId = body.get("responseId");
            PurchaseOrder order = service.createOrder(requestId, rfqId, responseId);
            return ResponseEntity.ok(order);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    /** GET /api/procurement/orders — All purchase orders */
    @GetMapping
    public ResponseEntity<List<PurchaseOrder>> getAll() {
        return ResponseEntity.ok(service.getAllOrders());
    }

    /** GET /api/procurement/orders/{id} — Single order */
    @GetMapping("/{id}")
    public ResponseEntity<PurchaseOrder> getById(@PathVariable String id) {
        return service.getOrderById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /** GET /api/procurement/orders/request/{requestId} — Order by request */
    @GetMapping("/request/{requestId}")
    public ResponseEntity<PurchaseOrder> getByRequest(@PathVariable String requestId) {
        return service.getOrderByRequestId(requestId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /** POST /api/procurement/orders/{id}/confirm-receipt — Confirm delivery */
    @BlockchainTraceable(action = "Confirm receipt of Equipments")
    @PostMapping("/{id}/confirm-receipt")
    public ResponseEntity<?> confirmReceipt(@PathVariable String id, @RequestBody Map<String, Object> body) {
        try {
            String notes = (String) body.get("notes");
            Integer rating = (Integer) body.get("rating");
            Boolean postToStock = (Boolean) body.getOrDefault("postToStock", false);
            PurchaseOrder order = service.confirmReceipt(id, notes, rating, postToStock);
            return ResponseEntity.ok(order);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }
}
