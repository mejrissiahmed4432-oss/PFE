package com.example.stockmanagermicroservice.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.ArrayList;
import java.util.List;

@Document(collection = "equipment_categories")
public class EquipmentCategory {
    @Id
    private String id;
    private String name;
    private String icon;
    private List<CategoryType> types = new ArrayList<>();

    public EquipmentCategory() {}

    public EquipmentCategory(String name, String icon) {
        this.name = name;
        this.icon = icon;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getIcon() { return icon; }
    public void setIcon(String icon) { this.icon = icon; }

    public List<CategoryType> getTypes() { return types; }
    public void setTypes(List<CategoryType> types) { this.types = types; }
}
