package com.example.aiservice.model;

public class AutocompleteRequest {
    private String text;

    public AutocompleteRequest() {}

    public AutocompleteRequest(String text) {
        this.text = text;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }
}
