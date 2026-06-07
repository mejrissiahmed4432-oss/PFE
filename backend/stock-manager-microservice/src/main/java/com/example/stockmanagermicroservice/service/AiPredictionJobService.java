package com.example.stockmanagermicroservice.service;

import com.example.stockmanagermicroservice.client.AiClient;
import com.example.stockmanagermicroservice.model.EquipmentMetricsSnapshot;
import com.example.stockmanagermicroservice.model.EquipmentPrediction;
import com.example.stockmanagermicroservice.repository.EquipmentMetricsSnapshotRepository;
import com.example.stockmanagermicroservice.repository.EquipmentPredictionRepository;
import com.example.stockmanagermicroservice.repository.EquipmentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class AiPredictionJobService {

    @Autowired
    private EquipmentRepository equipmentRepository;

    @Autowired
    private EquipmentMetricsSnapshotRepository snapshotRepository;

    @Autowired
    private EquipmentPredictionRepository predictionRepository;

    @Autowired
    private AiClient aiClient;

    @Scheduled(cron = "0 0 2 * * *") // Run at 2 AM every day
    public void generateDailyPredictions() {
        System.out.println("[AiPredictionJobService] Starting daily predictive maintenance job...");
        LocalDateTime sevenDaysAgo = LocalDateTime.now().minusDays(7);

        equipmentRepository.findAllExcludingFiles().stream()
                .filter(e -> e.getType() != null && e.getType().equalsIgnoreCase("laptop"))
                .forEach(equipment -> {
                    try {
                        List<EquipmentMetricsSnapshot> snapshots = snapshotRepository
                                .findByEquipmentIdAndTimestampAfterOrderByTimestampAsc(equipment.getId(), sevenDaysAgo);

                        if (snapshots.size() >= 5) { // Only predict if we have enough data (at least 5 snapshots)
                            // Limit to the last 24 snapshots to prevent LLM timeout / huge payload
                            List<EquipmentMetricsSnapshot> limitedSnapshots = snapshots;
                            if (snapshots.size() > 10) {
                                limitedSnapshots = snapshots.subList(snapshots.size() - 10, snapshots.size());
                            }

                            Map<String, Object> request = new HashMap<>();
                            request.put("equipmentId", equipment.getId());
                            request.put("serialNumber", equipment.getSerialNumber());
                            request.put("equipmentName", equipment.getEquipmentName());
                            request.put("department", equipment.getDepartment());
                            request.put("historicalMetrics", limitedSnapshots);

                            Map<String, Object> response = aiClient.predictMaintenance(request);

                            // Add a 3-second delay between AI service calls to prevent OpenRouter rate
                            // limits (429)
                            try {
                                Thread.sleep(3000);
                            } catch (InterruptedException ie) {
                                Thread.currentThread().interrupt();
                            }

                            EquipmentPrediction prediction = predictionRepository.findByEquipmentId(equipment.getId())
                                    .orElse(new EquipmentPrediction());

                            prediction.setEquipmentId(equipment.getId());
                            prediction.setSerialNumber(equipment.getSerialNumber());
                            prediction.setEquipmentName(equipment.getEquipmentName());
                            prediction.setDepartment(equipment.getDepartment());

                            prediction.setRiskLevel((String) response.get("riskLevel"));

                            // Safe casting for integer
                            Object riskScoreObj = response.get("riskScore");
                            if (riskScoreObj instanceof Integer) {
                                prediction.setRiskScore((Integer) riskScoreObj);
                            } else if (riskScoreObj instanceof String) {
                                prediction.setRiskScore(Integer.parseInt((String) riskScoreObj));
                            } else {
                                prediction.setRiskScore(0);
                            }

                            prediction.setPredictedIssues((List<String>) response.get("predictedIssues"));
                            prediction.setRecommendedActions((List<String>) response.get("recommendedActions"));
                            prediction.setLastAnalyzedAt(LocalDateTime.now());

                            predictionRepository.save(prediction);
                        }
                    } catch (feign.FeignException e) {
                        System.err.println("[AiPredictionJobService] Feign Error for " + equipment.getId()
                                + " - Status: " + e.status() + ", Body: " + e.contentUTF8());
                    } catch (Exception e) {
                        System.err.println("[AiPredictionJobService] Failed prediction for " + equipment.getId() + ": "
                                + e.getMessage());
                        if (e.getCause() != null) {
                            System.err.println("   Cause: " + e.getCause().getMessage());
                            if (e.getCause() instanceof feign.FeignException) {
                                feign.FeignException fe = (feign.FeignException) e.getCause();
                                System.err.println("   Feign Body: " + fe.contentUTF8());
                            }
                        }
                    }
                });
        System.out.println("[AiPredictionJobService] Daily predictive maintenance job completed.");
    }
}
