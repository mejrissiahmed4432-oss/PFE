package com.example.stockmanagermicroservice.controller;

import com.example.stockmanagermicroservice.model.Equipment;
import com.example.stockmanagermicroservice.service.EquipmentService;
import com.example.stockmanagermicroservice.config.BlockchainTraceable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/equipment")
public class EquipmentController {

    @Autowired
    private EquipmentService equipmentService;

    @GetMapping
    public ResponseEntity<?> getAllEquipment() {
        try {
            return ResponseEntity.ok(equipmentService.getAllEquipment());
        } catch (Exception e) {
            java.io.StringWriter sw = new java.io.StringWriter();
            java.io.PrintWriter pw = new java.io.PrintWriter(sw);
            e.printStackTrace(pw);
            return ResponseEntity.status(500).body("Error: " + e.getMessage() + "\nTrace: " + sw.toString());
        }
    }

    @GetMapping("/shelf/{shelfId}")
    public List<Equipment> getEquipmentByShelfId(@PathVariable String shelfId) {
        return equipmentService.getEquipmentByShelfId(shelfId);
    }

    @GetMapping("/check-serial/{serial}")
    public ResponseEntity<Boolean> checkSerialUnique(
            @PathVariable String serial,
            @RequestParam(required = false) String excludeId) {
        return ResponseEntity.ok(equipmentService.isSerialNumberUnique(serial, excludeId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Equipment> getEquipmentById(@PathVariable String id) {
        return equipmentService.getEquipmentById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @BlockchainTraceable(action = "Create Equipment")
    @PostMapping
    public ResponseEntity<Equipment> createEquipment(@RequestBody Equipment equipment) {
        return ResponseEntity.ok(equipmentService.createEquipment(equipment));
    }

    @BlockchainTraceable(action = "Bulk creation of Equipments")
    @PostMapping("/bulk")
    public List<Equipment> createBulkEquipment(@RequestBody List<Equipment> equipments) {
        return equipmentService.createBulkEquipment(equipments);
    }

    @GetMapping("/{id}/invoice-file")
    public ResponseEntity<String> getInvoiceFile(@PathVariable String id) {
        return equipmentService.getEquipmentFiles(id)
                .map(eq -> ResponseEntity.ok(eq.getInvoiceFileData()))
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/{id}/warranty-file")
    public ResponseEntity<String> getWarrantyFile(@PathVariable String id) {
        return equipmentService.getEquipmentFiles(id)
                .map(eq -> ResponseEntity.ok(eq.getWarrantyFileData()))
                .orElse(ResponseEntity.notFound().build());
    }

    @BlockchainTraceable(action = "Update Equipment")
    @PutMapping("/{id}")
    public ResponseEntity<Equipment> updateEquipment(@PathVariable String id, @RequestBody Equipment equipment) {
        try {
            return ResponseEntity.ok(equipmentService.updateEquipment(id, equipment));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @BlockchainTraceable(action = "Delete Equipment")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteEquipment(@PathVariable String id) {
        equipmentService.deleteEquipment(id);
        return ResponseEntity.noContent().build();
    }

    @BlockchainTraceable(action = "Bulk delete of Equipments")
    @PostMapping("/bulk-delete")
    public ResponseEntity<Void> deleteBulk(@RequestBody List<String> ids) {
        equipmentService.deleteBulk(ids);
        return ResponseEntity.noContent().build();
    }

    @BlockchainTraceable(action = "Bulk update of Equipments")
    @PutMapping("/bulk-update-basic")
    public ResponseEntity<List<Equipment>> updateBulkBasicInfo(@RequestBody BulkUpdateBasicRequest request) {
        List<Equipment> updated = equipmentService.updateBulkBasicInfo(
                request.ids, request.name, request.brand, request.model);
        return ResponseEntity.ok(updated);
    }

    public static class BulkUpdateBasicRequest {
        public List<String> ids;
        public String name;
        public String brand;
        public String model;
    }

    @BlockchainTraceable(action = "Consume Parts")
    @PostMapping("/consume-parts/{requesterId}")
    public ResponseEntity<Void> consumeParts(@PathVariable String requesterId,
            @RequestBody List<PartConsumeRequest> requests) {
        equipmentService.consumeParts(requesterId, requests);
        return ResponseEntity.ok().build();
    }

    @BlockchainTraceable(action = "Allocate Parts")
    @PostMapping("/allocate-parts")
    public ResponseEntity<Void> allocateParts(@RequestBody PartAllocateRequest request) {
        equipmentService.allocateParts(request.requesterId, request.requesterName, request.parts);
        return ResponseEntity.ok().build();
    }

    @BlockchainTraceable(action = "Return Part")
    @PostMapping("/{id}/return")
    public ResponseEntity<Void> returnPart(@PathVariable String id) {
        equipmentService.returnPartToStock(id);
        return ResponseEntity.ok().build();
    }

    /**
     * One-time cleanup: removes "Out of Stock" records that have the same serial
     * number
     * as an existing "Allocated" record. These were created by a previous bug in
     * allocateParts.
     */
    @DeleteMapping("/cleanup-duplicates")
    public ResponseEntity<String> cleanupDuplicates() {
        int removed = equipmentService.cleanupDuplicateOutOfStockRecords();
        return ResponseEntity.ok("Removed " + removed + " duplicate Out of Stock record(s).");
    }

    // ── IT Manager Equipment Management Endpoints ─────────────────────────

    /** GET available equipment (status=Available, dept=stock) for IT Manager */
    @GetMapping("/it-available")
    public ResponseEntity<?> getAvailableInStock() {
        try {
            return ResponseEntity.ok(equipmentService.getAvailableInStock());
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error: " + e.getMessage());
        }
    }

    /** GET all equipment currently In Use */
    @GetMapping("/it-in-use")
    public ResponseEntity<?> getAllInUse() {
        try {
            return ResponseEntity.ok(equipmentService.getAllInUse());
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error: " + e.getMessage());
        }
    }

    /** GET equipment with assignment history (has been assigned at least once) */
    @GetMapping("/it-assignment-history")
    public ResponseEntity<?> getAssignmentHistory() {
        try {
            return ResponseEntity.ok(equipmentService.getAssignmentHistory());
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error: " + e.getMessage());
        }
    }

    /** GET all pending return requests (for Stock Manager) */
    @GetMapping("/return-requests")
    public ResponseEntity<?> getReturnRequests() {
        try {
            return ResponseEntity.ok(equipmentService.getReturnRequests());
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error: " + e.getMessage());
        }
    }

    /** POST assign equipment to users or department (IT Manager) */
    @BlockchainTraceable(action = "Assign Equipment")
    @PostMapping("/{id}/it-assign")
    public ResponseEntity<?> assignEquipmentIT(
            @PathVariable String id,
            @RequestBody java.util.Map<String, Object> request) {
        try {
            return ResponseEntity.ok(equipmentService.assignEquipmentIT(id, request));
        } catch (Exception e) {
            return ResponseEntity.status(400).body("Error: " + e.getMessage());
        }
    }

    /** POST deassign equipment from users/department (IT Manager) */
    @BlockchainTraceable(action = "Unassign Equipment")
    @PostMapping("/{id}/it-deassign")
    public ResponseEntity<?> deassignEquipmentIT(
            @PathVariable String id,
            @RequestBody(required = false) java.util.Map<String, String> body) {
        try {
            String actor = body != null ? body.getOrDefault("actor", "IT Manager") : "IT Manager";
            return ResponseEntity.ok(equipmentService.deassignEquipmentIT(id, actor));
        } catch (Exception e) {
            return ResponseEntity.status(400).body("Error: " + e.getMessage());
        }
    }

    /** POST request return of equipment to stock (IT Manager) */

    @PostMapping("/{id}/request-return")
    public ResponseEntity<?> requestReturn(
            @PathVariable String id,
            @RequestBody java.util.Map<String, String> body) {
        try {
            String note = body.getOrDefault("note", "");
            String actor = body.getOrDefault("actor", "IT Manager");
            return ResponseEntity.ok(equipmentService.requestReturnToIT(id, note, actor));
        } catch (Exception e) {
            return ResponseEntity.status(400).body("Error: " + e.getMessage());
        }
    }

    /** POST process a return request (Stock Manager) */
    @BlockchainTraceable(action = "Equipment Return")
    @PostMapping("/{id}/process-return")
    public ResponseEntity<?> processReturn(
            @PathVariable String id,
            @RequestBody java.util.Map<String, String> body) {
        try {
            String newStatus = body.getOrDefault("status", "Available");
            String shelfId = body.getOrDefault("shelfId", null);
            String actor = body.getOrDefault("actor", "Stock Manager");
            return ResponseEntity.ok(equipmentService.processReturnRequest(id, newStatus, shelfId, actor));
        } catch (Exception e) {
            return ResponseEntity.status(400).body("Error: " + e.getMessage());
        }
    }

    public static class PartAllocateRequest {
        public String requesterId;
        public String requesterName;
        public List<PartConsumeRequest> parts;
    }

    public static class PartConsumeRequest {
        public String name;
        public String brand;
        public String type;
        public java.util.Map<String, String> specifications;
        public int qty;
        public String equipmentId;
        public String assignedToEquipmentName;
        public String assignedToEquipmentId;
    }
}
