package com.example.aiservice.model;

import java.util.List;

public class EquipmentParsingResponse {
    private List<ParsedItem> items;
    private boolean success;
    private String error;

    public EquipmentParsingResponse() {}

    public EquipmentParsingResponse(List<ParsedItem> items, boolean success) {
        this.items = items;
        this.success = success;
    }

    public static EquipmentParsingResponse error(String error) {
        EquipmentParsingResponse response = new EquipmentParsingResponse();
        response.setSuccess(false);
        response.setError(error);
        return response;
    }

    public List<ParsedItem> getItems() {
        return items;
    }

    public void setItems(List<ParsedItem> items) {
        this.items = items;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }
}
