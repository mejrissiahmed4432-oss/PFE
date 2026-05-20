package com.example.stockmanagermicroservice.service;

import com.example.stockmanagermicroservice.dto.DeptPcSummaryDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Broadcasts real-time department monitoring data over WebSocket every 2 seconds.
 * Frontend subscribes to /topic/dept-monitoring to receive live updates.
 */
@Service
public class MonitoringBroadcastService {

    @Autowired
    private SimpMessagingTemplate template;

    @Autowired
    private LaptopMonitoringService laptopMonitoringService;

    @Scheduled(fixedRate = 2000)
    public void broadcastDeptStatus() {
        try {
            List<DeptPcSummaryDTO> data = laptopMonitoringService.getDeptPcStatus();
            template.convertAndSend("/topic/dept-monitoring", data);
        } catch (Exception e) {
            System.err.println("[MonitoringBroadcastService] Error: " + e.getMessage());
        }
    }
}
