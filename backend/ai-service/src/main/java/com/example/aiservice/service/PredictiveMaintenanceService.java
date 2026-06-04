package com.example.aiservice.service;

import com.example.aiservice.model.PredictiveMaintenanceRequest;
import com.example.aiservice.model.PredictiveMaintenanceResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.converter.BeanOutputConverter;
import org.springframework.stereotype.Service;

@Service
public class PredictiveMaintenanceService {

    private static final Logger log = LoggerFactory.getLogger(PredictiveMaintenanceService.class);
    private final ChatModel chatModel;

    public PredictiveMaintenanceService(ChatModel chatModel) {
        this.chatModel = chatModel;
    }

    public PredictiveMaintenanceResponse predict(PredictiveMaintenanceRequest request) {
        try {
            BeanOutputConverter<PredictiveMaintenanceResponse> converter =
                    new BeanOutputConverter<>(PredictiveMaintenanceResponse.class);

            String format = converter.getFormat();

            String systemPrompt = """
                    You are an expert IT Predictive Maintenance AI.
                    Analyze the provided historical metrics for an equipment and predict potential failures.
                    
                    EQUIPMENT DETAILS:
                    Name: {name}
                    Serial: {serial}
                    Department: {department}
                    
                    HISTORICAL METRICS:
                    {metrics}
                    
                    INSTRUCTIONS:
                    1. Analyze the trends (e.g., high CPU, increasing temperature, low disk space).
                    2. Calculate a risk score from 0 to 100 (100 being imminent failure).
                    3. Determine the risk level: LOW (0-30), MEDIUM (31-60), HIGH (61-85), CRITICAL (86-100).
                    4. List specific predicted issues based on the metrics.
                    5. Provide actionable recommended actions for the technician to prevent failures.
                    
                    {format}
                    """;

            ObjectMapper mapper = new ObjectMapper();
            String metricsJson = mapper.writeValueAsString(request.getHistoricalMetrics());

            PromptTemplate template = new PromptTemplate(systemPrompt);
            template.add("name", request.getEquipmentName() != null ? request.getEquipmentName() : "Unknown");
            template.add("serial", request.getSerialNumber() != null ? request.getSerialNumber() : "Unknown");
            template.add("department", request.getDepartment() != null ? request.getDepartment() : "Unknown");
            template.add("metrics", metricsJson);
            template.add("format", format);

            Prompt prompt = template.create();
            String responseString = chatModel.call(prompt).getResult().getOutput().getText();

            return converter.convert(responseString);
            
        } catch (Exception e) {
            log.error("Failed to generate predictive maintenance report: {}", e.getMessage(), e);
            PredictiveMaintenanceResponse fallback = new PredictiveMaintenanceResponse();
            fallback.setRiskLevel("LOW");
            fallback.setRiskScore(0);
            fallback.setPredictedIssues(java.util.List.of("Error analyzing metrics: " + e.getMessage()));
            fallback.setRecommendedActions(java.util.List.of("Check AI service logs"));
            return fallback;
        }
    }
}
