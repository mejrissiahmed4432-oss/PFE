package com.example.stockmanagermicroservice.controller;

import com.example.stockmanagermicroservice.config.BlockchainTraceable;
import com.example.stockmanagermicroservice.dto.LicensePoolDTO;
import com.example.stockmanagermicroservice.dto.SoftwareAssignmentDTO;
import com.example.stockmanagermicroservice.dto.SoftwareDTO;
import com.example.stockmanagermicroservice.service.AssignmentService;
import com.example.stockmanagermicroservice.service.LicensePoolService;
import com.example.stockmanagermicroservice.service.SoftwareService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/software")
@CrossOrigin(origins = "*")
public class SoftwareController {

    @Autowired
    private SoftwareService softwareService;

    @Autowired
    private LicensePoolService licensePoolService;

    @Autowired
    private AssignmentService assignmentService;

    // --- SOFTWARE ENDPOINTS ---

    @GetMapping
    public ResponseEntity<List<SoftwareDTO>> getAllSoftware() {
        return ResponseEntity.ok(softwareService.getAllSoftware());
    }

    @GetMapping("/{id}")
    public ResponseEntity<SoftwareDTO> getSoftwareById(@PathVariable String id) {
        return ResponseEntity.ok(softwareService.getSoftwareById(id));
    }

    @GetMapping("/available")
    public ResponseEntity<List<SoftwareDTO>> getAvailableSoftware() {
        List<SoftwareDTO> available = softwareService.getAllSoftware().stream()
            .filter(s -> s.getAvailableSeats() > 0 && !"Deprecated".equalsIgnoreCase(s.getStatus()))
            .toList();
        return ResponseEntity.ok(available);
    }

    @BlockchainTraceable(action = "create software")
    @PostMapping
    public ResponseEntity<SoftwareDTO> createSoftware(@RequestBody SoftwareDTO dto) {
        return ResponseEntity.ok(softwareService.createSoftware(dto));
    }

    @BlockchainTraceable(action = "update software")
    @PutMapping("/{id}")
    public ResponseEntity<SoftwareDTO> updateSoftware(@PathVariable String id, @RequestBody SoftwareDTO dto) {
        return ResponseEntity.ok(softwareService.updateSoftware(id, dto));
    }

    @BlockchainTraceable(action = "delete software")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteSoftware(@PathVariable String id) {
        softwareService.deleteSoftware(id);
        return ResponseEntity.ok().build();
    }

    // --- LICENSE POOL ENDPOINTS ---

    @GetMapping("/{softwareId}/pools")
    public ResponseEntity<List<LicensePoolDTO>> getPoolsBySoftware(@PathVariable String softwareId) {
        return ResponseEntity.ok(licensePoolService.getPoolsBySoftwareId(softwareId));
    }

    @PostMapping("/{softwareId}/pools")
    public ResponseEntity<LicensePoolDTO> createLicensePool(@PathVariable String softwareId,
            @RequestBody LicensePoolDTO dto) {
        dto.setSoftwareId(softwareId);
        return ResponseEntity.ok(licensePoolService.createLicensePool(dto));
    }

    @PostMapping("/pools/{poolId}/reveal-keys")
    public ResponseEntity<List<String>> revealKeys(@PathVariable String poolId,
            @RequestBody Map<String, String> request) {
        String password = request.get("password");
        return ResponseEntity.ok(licensePoolService.revealKeys(poolId, password));
    }

    // --- ASSIGNMENT ENDPOINTS ---

    @GetMapping("/{softwareId}/assignments")
    public ResponseEntity<List<SoftwareAssignmentDTO>> getAssignmentsBySoftware(@PathVariable String softwareId) {
        return ResponseEntity.ok(assignmentService.getAssignmentsBySoftware(softwareId));
    }

    @BlockchainTraceable(action = "assign license")
    @PostMapping("/assignments")
    public ResponseEntity<SoftwareAssignmentDTO> assignLicense(@RequestBody SoftwareAssignmentDTO dto) {
        return ResponseEntity.ok(assignmentService.assignLicense(dto));
    }

    @BlockchainTraceable(action = "revoke assignment")
    @PostMapping("/assignments/{assignmentId}/revoke")
    public ResponseEntity<SoftwareAssignmentDTO> revokeAssignment(@PathVariable String assignmentId) {
        return ResponseEntity.ok(assignmentService.revokeAssignment(assignmentId));
    }
}
