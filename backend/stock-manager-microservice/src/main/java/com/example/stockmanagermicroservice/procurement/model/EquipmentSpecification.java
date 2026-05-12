package com.example.stockmanagermicroservice.procurement.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.util.List;

@Document(collection = "equipment_specifications")
public class EquipmentSpecification {

    @Id
    private String id;
    private String catalogItemId; // Link to CatalogItem
    private String name; // e.g., "RAM", "CPU", "Storage"
    private List<String> possibleValues; // e.g., ["8GB", "16GB", "32GB"]

    public EquipmentSpecification() {}

    public EquipmentSpecification(String catalogItemId, String name, List<String> possibleValues) {
        this.catalogItemId = catalogItemId;
        this.name = name;
        this.possibleValues = possibleValues;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getCatalogItemId() { return catalogItemId; }
    public void setCatalogItemId(String catalogItemId) { this.catalogItemId = catalogItemId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public List<String> getPossibleValues() { return possibleValues; }
    public void setPossibleValues(List<String> possibleValues) { this.possibleValues = possibleValues; }
}
