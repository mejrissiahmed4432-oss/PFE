package com.example.stockmanagermicroservice.procurement.model;

public class EquipmentRequestItem {

    private String name;
    private int quantity;
    private String description;
    private String catalogItemId; // Optional link to a catalog item
    private java.util.Map<String, String> selectedSpecs;

    public EquipmentRequestItem() {
        this.selectedSpecs = new java.util.HashMap<>();
    }

    public EquipmentRequestItem(String name, int quantity, String description, String catalogItemId) {
        this.name = name;
        this.quantity = quantity;
        this.description = description;
        this.catalogItemId = catalogItemId;
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getCatalogItemId() { return catalogItemId; }
    public void setCatalogItemId(String catalogItemId) { this.catalogItemId = catalogItemId; }

    public java.util.Map<String, String> getSelectedSpecs() { return selectedSpecs; }
    public void setSelectedSpecs(java.util.Map<String, String> selectedSpecs) { this.selectedSpecs = selectedSpecs; }
}
