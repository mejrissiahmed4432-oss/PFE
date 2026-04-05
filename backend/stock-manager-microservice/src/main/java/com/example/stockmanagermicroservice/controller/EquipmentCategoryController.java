package com.example.stockmanagermicroservice.controller;

import com.example.stockmanagermicroservice.model.EquipmentCategory;
import com.example.stockmanagermicroservice.service.EquipmentCategoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/equipment-categories")
public class EquipmentCategoryController {

    @Autowired
    private EquipmentCategoryService service;

    @GetMapping
    public ResponseEntity<List<EquipmentCategory>> getAllCategories() {
        return ResponseEntity.ok(service.getAllCategories());
    }

    @GetMapping("/{id}")
    public ResponseEntity<EquipmentCategory> getCategoryById(@PathVariable String id) {
        return service.getCategoryById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<?> createCategory(@RequestBody EquipmentCategory category) {
        try {
            return ResponseEntity.ok(service.createOrUpdateCategory(category));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(409).body(e.getMessage());
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateCategory(@PathVariable String id, @RequestBody EquipmentCategory category) {
        try {
            category.setId(id);
            return ResponseEntity.ok(service.createOrUpdateCategory(category));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(409).body(e.getMessage());
        }
    }

    @PostMapping("/{id}/types")
    public ResponseEntity<?> addTypeToCategory(@PathVariable String id, @RequestBody String type) {
        try {
            return ResponseEntity.ok(service.addTypeToCategory(id, type));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(409).body(e.getMessage());
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{id}/types/{type}")
    public ResponseEntity<EquipmentCategory> removeTypeFromCategory(@PathVariable String id, @PathVariable String type) {
        try {
            return ResponseEntity.ok(service.removeTypeFromCategory(id, type));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteCategory(@PathVariable String id) {
        try {
            service.deleteCategory(id);
            return ResponseEntity.noContent().build();
        } catch (IllegalStateException e) {
            return ResponseEntity.status(409).body(e.getMessage());
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }
}
