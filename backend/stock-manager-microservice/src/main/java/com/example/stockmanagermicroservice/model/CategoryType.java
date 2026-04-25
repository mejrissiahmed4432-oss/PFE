package com.example.stockmanagermicroservice.model;

import java.util.ArrayList;
import java.util.List;

public class CategoryType {
    private String name;
    private boolean requiresQrCode;
    private List<String> specificationFields = new ArrayList<>();

    public CategoryType() {}

    public CategoryType(String name, boolean requiresQrCode) {
        this.name = name;
        this.requiresQrCode = requiresQrCode;
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public boolean isRequiresQrCode() { return requiresQrCode; }
    public void setRequiresQrCode(boolean requiresQrCode) { this.requiresQrCode = requiresQrCode; }

    public List<String> getSpecificationFields() { return specificationFields; }
    public void setSpecificationFields(List<String> specificationFields) { this.specificationFields = specificationFields; }
}
