package com.medina.app.model;

public class PartRequestItem {
    private String partName;
    private String category;
    private Integer quantity;
    private String type;
    private String specification;
    private String equipmentId;
    private String brand;
    private String matchedEquipmentName;
    private String matchedSpecification;
    private String matchedSerialNumber;
    private Boolean processed;
    private Boolean returned;

    public PartRequestItem() {}

    public String getPartName() { return partName; }
    public void setPartName(String partName) { this.partName = partName; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getSpecification() { return specification; }
    public void setSpecification(String specification) { this.specification = specification; }

    public String getEquipmentId() { return equipmentId; }
    public void setEquipmentId(String equipmentId) { this.equipmentId = equipmentId; }

    public String getBrand() { return brand; }
    public void setBrand(String brand) { this.brand = brand; }

    public String getMatchedEquipmentName() { return matchedEquipmentName; }
    public void setMatchedEquipmentName(String matchedEquipmentName) { this.matchedEquipmentName = matchedEquipmentName; }

    public String getMatchedSpecification() { return matchedSpecification; }
    public void setMatchedSpecification(String matchedSpecification) { this.matchedSpecification = matchedSpecification; }

    public String getMatchedSerialNumber() { return matchedSerialNumber; }
    public void setMatchedSerialNumber(String matchedSerialNumber) { this.matchedSerialNumber = matchedSerialNumber; }

    public Boolean getProcessed() { return processed; }
    public void setProcessed(Boolean processed) { this.processed = processed; }

    public Boolean getReturned() { return returned; }
    public void setReturned(Boolean returned) { this.returned = returned; }
}
