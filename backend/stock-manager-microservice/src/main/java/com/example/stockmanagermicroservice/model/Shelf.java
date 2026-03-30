package com.example.stockmanagermicroservice.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "shelves")
public class Shelf {
    @Id
    private String id;
    
    private String nb;
    private Integer maxQte;
    private Integer currentQte;
    private String status; // EMPTY, NORMAL, FULL
    private String equipmentType;

    public Shelf() {
        this.currentQte = 0;
        this.status = "EMPTY";
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getNb() { return nb; }
    public void setNb(String nb) { this.nb = nb; }

    public Integer getMaxQte() { return maxQte; }
    public void setMaxQte(Integer maxQte) { this.maxQte = maxQte; }

    public Integer getCurrentQte() { return currentQte; }
    public void setCurrentQte(Integer currentQte) { this.currentQte = currentQte; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getEquipmentType() { return equipmentType; }
    public void setEquipmentType(String equipmentType) { this.equipmentType = equipmentType; }
}
