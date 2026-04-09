package com.example.stockmanagermicroservice.model;

public class CategoryType {
    private String name;
    private boolean requiresQrCode;

    public CategoryType() {}

    public CategoryType(String name, boolean requiresQrCode) {
        this.name = name;
        this.requiresQrCode = requiresQrCode;
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public boolean isRequiresQrCode() { return requiresQrCode; }
    public void setRequiresQrCode(boolean requiresQrCode) { this.requiresQrCode = requiresQrCode; }
}
