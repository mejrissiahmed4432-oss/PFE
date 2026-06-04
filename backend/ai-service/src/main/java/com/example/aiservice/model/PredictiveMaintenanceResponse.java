package com.example.aiservice.model;

import java.util.List;

public class PredictiveMaintenanceResponse {
    private String riskLevel; // LOW, MEDIUM, HIGH, CRITICAL
    private int riskScore; // 0-100
    private List<String> predictedIssues;
    private List<String> recommendedActions;

    public String getRiskLevel() { return riskLevel; }
    public void setRiskLevel(String riskLevel) { this.riskLevel = riskLevel; }

    public int getRiskScore() { return riskScore; }
    public void setRiskScore(int riskScore) { this.riskScore = riskScore; }

    public List<String> getPredictedIssues() { return predictedIssues; }
    public void setPredictedIssues(List<String> predictedIssues) { this.predictedIssues = predictedIssues; }

    public List<String> getRecommendedActions() { return recommendedActions; }
    public void setRecommendedActions(List<String> recommendedActions) { this.recommendedActions = recommendedActions; }
}
