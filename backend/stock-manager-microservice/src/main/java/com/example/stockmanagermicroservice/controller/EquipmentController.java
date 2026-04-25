package com.example.stockmanagermicroservice.controller;

import com.example.stockmanagermicroservice.model.Equipment;
import com.example.stockmanagermicroservice.service.EquipmentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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

    @PostMapping
    public Equipment createEquipment(@RequestBody Equipment equipment) {
        return equipmentService.createEquipment(equipment);
    }

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

    @PutMapping("/{id}")
    public ResponseEntity<Equipment> updateEquipment(@PathVariable String id, @RequestBody Equipment equipment) {
        try {
            return ResponseEntity.ok(equipmentService.updateEquipment(id, equipment));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteEquipment(@PathVariable String id) {
        equipmentService.deleteEquipment(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/bulk-delete")
    public ResponseEntity<Void> deleteBulk(@RequestBody List<String> ids) {
        equipmentService.deleteBulk(ids);
        return ResponseEntity.noContent().build();
    }

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

    @PostMapping("/consume-parts/{requesterId}")
    public ResponseEntity<Void> consumeParts(@PathVariable String requesterId, @RequestBody List<PartConsumeRequest> requests) {
        equipmentService.consumeParts(requesterId, requests);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/allocate-parts")
    public ResponseEntity<Void> allocateParts(@RequestBody PartAllocateRequest request) {
        equipmentService.allocateParts(request.requesterId, request.requesterName, request.parts);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{id}/return")
    public ResponseEntity<Void> returnPart(@PathVariable String id) {
        equipmentService.returnPartToStock(id);
        return ResponseEntity.ok().build();
    }

    /**
     * One-time cleanup: removes "Out of Stock" records that have the same serial number
     * as an existing "Allocated" record. These were created by a previous bug in allocateParts.
     */
    @DeleteMapping("/cleanup-duplicates")
    public ResponseEntity<String> cleanupDuplicates() {
        int removed = equipmentService.cleanupDuplicateOutOfStockRecords();
        return ResponseEntity.ok("Removed " + removed + " duplicate Out of Stock record(s).");
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
