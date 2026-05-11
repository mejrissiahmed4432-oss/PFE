package com.example.stockmanagermicroservice.procurement.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.List;

@Document(collection = "catalog_items")
public class CatalogItem {

    @Id
    private String id;
    private String name;
    private String category;
    private String description;
    private String defaultSpecs;
    private List<String> relatedTags;
    private List<String> defaultSuppliers;

    public CatalogItem() {
    }

    public CatalogItem(String name, String category, String description, String defaultSpecs, List<String> relatedTags, List<String> defaultSuppliers) {
        this.name = name;
        this.category = category;
        this.description = description;
        this.defaultSpecs = defaultSpecs;
        this.relatedTags = relatedTags;
        this.defaultSuppliers = defaultSuppliers;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getDefaultSpecs() {
        return defaultSpecs;
    }

    public void setDefaultSpecs(String defaultSpecs) {
        this.defaultSpecs = defaultSpecs;
    }

    public List<String> getRelatedTags() {
        return relatedTags;
    }

    public void setRelatedTags(List<String> relatedTags) {
        this.relatedTags = relatedTags;
    }

    public List<String> getDefaultSuppliers() {
        return defaultSuppliers;
    }

    public void setDefaultSuppliers(List<String> defaultSuppliers) {
        this.defaultSuppliers = defaultSuppliers;
    }
}
