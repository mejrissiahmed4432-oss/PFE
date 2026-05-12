package com.example.aiservice.model;

import jakarta.validation.constraints.NotBlank;

public class EquipmentParsingRequest {
    @NotBlank(message = "Text to parse is required")
    private String text;

    public EquipmentParsingRequest() {}

    public EquipmentParsingRequest(String text) {
        this.text = text;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }
}
