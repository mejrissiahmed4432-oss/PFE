package com.example.stockmanagermicroservice.controller;

import com.example.stockmanagermicroservice.dto.InstallOSRequest;
import com.example.stockmanagermicroservice.dto.OperatingSystemDTO;
import com.example.stockmanagermicroservice.model.EquipmentSoftware;
import com.example.stockmanagermicroservice.service.OperatingSystemService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/os")
public class OperatingSystemController {

    @Autowired
    private OperatingSystemService osService;

    @GetMapping
    public ResponseEntity<List<OperatingSystemDTO>> getAllOperatingSystems() {
        return ResponseEntity.ok(osService.getAllOperatingSystems());
    }

    @PostMapping
    public ResponseEntity<OperatingSystemDTO> addOperatingSystem(@RequestBody OperatingSystemDTO dto) {
        return ResponseEntity.ok(osService.addOperatingSystem(dto));
    }

    @PutMapping("/{id}")
    public ResponseEntity<OperatingSystemDTO> updateOperatingSystem(@PathVariable String id, @RequestBody OperatingSystemDTO dto) {
        return ResponseEntity.ok(osService.updateOperatingSystem(id, dto));
    }

    @PostMapping("/install")
    public ResponseEntity<EquipmentSoftware> installOS(@RequestBody InstallOSRequest request) {
        return ResponseEntity.ok(osService.installOS(request));
    }

    @PostMapping("/uninstall/{softwareId}")
    public ResponseEntity<Void> uninstallOS(@PathVariable String softwareId) {
        osService.uninstallOS(softwareId);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteOperatingSystem(@PathVariable String id) {
        osService.deleteOperatingSystem(id);
        return ResponseEntity.ok().build();
    }
}
