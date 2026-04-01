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
    public List<Equipment> getAllEquipment() {
        return equipmentService.getAllEquipment();
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
}
