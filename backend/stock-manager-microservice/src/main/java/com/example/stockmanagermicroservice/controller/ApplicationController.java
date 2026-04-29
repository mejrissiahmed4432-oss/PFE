package com.example.stockmanagermicroservice.controller;

import com.example.stockmanagermicroservice.dto.ApplicationDTO;
import com.example.stockmanagermicroservice.dto.InstallApplicationRequest;
import com.example.stockmanagermicroservice.model.EquipmentApplication;
import com.example.stockmanagermicroservice.service.ApplicationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/applications")
public class ApplicationController {

    @Autowired
    private ApplicationService applicationService;

    @GetMapping
    public ResponseEntity<List<ApplicationDTO>> getAllApplications() {
        return ResponseEntity.ok(applicationService.getAllApplications());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApplicationDTO> getApplicationById(@PathVariable String id) {
        return ResponseEntity.ok(applicationService.getApplicationById(id));
    }

    @PostMapping
    public ResponseEntity<ApplicationDTO> createApplication(@RequestBody ApplicationDTO dto) {
        return ResponseEntity.ok(applicationService.createApplication(dto));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApplicationDTO> updateApplication(@PathVariable String id, @RequestBody ApplicationDTO dto) {
        return ResponseEntity.ok(applicationService.updateApplication(id, dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteApplication(@PathVariable String id) {
        applicationService.deleteApplication(id);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/install")
    public ResponseEntity<EquipmentApplication> installApplication(@RequestBody InstallApplicationRequest request) {
        return ResponseEntity.ok(applicationService.installApplication(request));
    }

    @PostMapping("/uninstall/{installationId}")
    public ResponseEntity<Void> uninstallApplication(@PathVariable String installationId) {
        applicationService.uninstallApplication(installationId);
        return ResponseEntity.ok().build();
    }
}
