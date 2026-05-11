package com.example.stockmanagermicroservice.procurement.controller;

import com.example.stockmanagermicroservice.procurement.model.EquipmentSpecification;
import com.example.stockmanagermicroservice.procurement.service.EquipmentSpecificationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/procurement/catalog")
@CrossOrigin(origins = "*")
public class EquipmentSpecificationController {

    private final EquipmentSpecificationService service;

    public EquipmentSpecificationController(EquipmentSpecificationService service) {
        this.service = service;
    }

    @GetMapping("/{catalogItemId}/specifications")
    public ResponseEntity<List<EquipmentSpecification>> getSpecifications(@PathVariable String catalogItemId) {
        return ResponseEntity.ok(service.getSpecsForCatalogItem(catalogItemId));
    }

    @PostMapping("/{catalogItemId}/specifications")
    public ResponseEntity<EquipmentSpecification> addSpecification(@PathVariable String catalogItemId, @RequestBody EquipmentSpecification spec) {
        spec.setCatalogItemId(catalogItemId);
        return ResponseEntity.ok(service.saveSpecification(spec));
    }
}
