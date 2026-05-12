package com.example.aiservice.model;

import java.util.List;

public class QuotationAnalysisResponse {
    private String recommendedSupplier;
    private String reasoning;
    private List<String> keyPros;
    private List<String> keyCons;
    private String summary;

    public QuotationAnalysisResponse() {}

    public String getRecommendedSupplier() { return recommendedSupplier; }
    public void setRecommendedSupplier(String recommendedSupplier) { this.recommendedSupplier = recommendedSupplier; }

    public String getReasoning() { return reasoning; }
    public void setReasoning(String reasoning) { this.reasoning = reasoning; }

    public List<String> getKeyPros() { return keyPros; }
    public void setKeyPros(List<String> keyPros) { this.keyPros = keyPros; }

    public List<String> getKeyCons() { return keyCons; }
    public void setKeyCons(List<String> keyCons) { this.keyCons = keyCons; }

    public String getSummary() { return summary; }
    public void setSummary(String summary) { this.summary = summary; }
}
