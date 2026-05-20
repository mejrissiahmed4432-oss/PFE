package com.medina.app.model;

import java.util.Map;

public class Equipment {
    private String id;
    private String equipmentName;
    private String brand;
    private String model;
    private String serialNumber;
    private String category;
    private String type;
    private Integer qte;
    private String status;
    private String shelfId;
    private Map<String, String> specifications;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getEquipmentName() { return equipmentName; }
    public void setEquipmentName(String equipmentName) { this.equipmentName = equipmentName; }

    public String getBrand() { return brand; }
    public void setBrand(String brand) { this.brand = brand; }

    public String getModel() { return model; }
    public void setModel(String model) { this.model = model; }

    public String getSerialNumber() { return serialNumber; }
    public void setSerialNumber(String serialNumber) { this.serialNumber = serialNumber; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public Integer getQte() { return qte; }
    public void setQte(Integer qte) { this.qte = qte; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getShelfId() { return shelfId; }
    public void setShelfId(String shelfId) { this.shelfId = shelfId; }

    public Map<String, String> getSpecifications() { return specifications; }
    public void setSpecifications(Map<String, String> specifications) { this.specifications = specifications; }
}
