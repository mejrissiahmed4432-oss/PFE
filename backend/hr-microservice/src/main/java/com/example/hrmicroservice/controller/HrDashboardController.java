package com.example.hrmicroservice.controller;

import com.example.hrmicroservice.dto.HrDashboardStats;
import com.example.hrmicroservice.service.HrService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/hr")
public class HrDashboardController {

    @Autowired
    private HrService hrService;

    @GetMapping("/dashboard/stats")
    public ResponseEntity<HrDashboardStats> getDashboardStats() {
        return ResponseEntity.ok(hrService.getDashboardStats());
    }
}
