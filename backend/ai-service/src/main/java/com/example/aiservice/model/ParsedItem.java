package com.example.aiservice.model;

public class ParsedItem {
    private String name;
    private int quantity;
    private String inferredCategory;
    private java.util.Map<String, String> detectedSpecs;

    public ParsedItem() {
        this.detectedSpecs = new java.util.HashMap<>();
    }

    public ParsedItem(String name, int quantity, String inferredCategory, java.util.Map<String, String> detectedSpecs) {
        this.name = name;
        this.quantity = quantity;
        this.inferredCategory = inferredCategory;
        this.detectedSpecs = detectedSpecs != null ? detectedSpecs : new java.util.HashMap<>();
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public String getInferredCategory() {
        return inferredCategory;
    }

    public void setInferredCategory(String inferredCategory) {
        this.inferredCategory = inferredCategory;
    }

    public java.util.Map<String, String> getDetectedSpecs() {
        return detectedSpecs;
    }

    public void setDetectedSpecs(java.util.Map<String, String> detectedSpecs) {
        this.detectedSpecs = detectedSpecs;
    }
}
