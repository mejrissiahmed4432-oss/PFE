package com.example.stockmanagermicroservice.controller;

import com.example.stockmanagermicroservice.dto.DeptPcSummaryDTO;
import com.example.stockmanagermicroservice.service.LaptopMonitoringService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST endpoint for the technician departments PC monitoring dashboard.
 */
@RestController
@RequestMapping("/api/monitoring")
public class MonitoringController {

    @Autowired
    private LaptopMonitoringService laptopMonitoringService;

    /**
     * Returns per-department laptop status summary.
     * GET /api/monitoring/dept-pc-status
     */
    @GetMapping("/dept-pc-status")
    public ResponseEntity<List<DeptPcSummaryDTO>> getDeptPcStatus() {
        try {
            List<DeptPcSummaryDTO> result = laptopMonitoringService.getDeptPcStatus();
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            System.err.println("[MonitoringController] Error: " + e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }
}
