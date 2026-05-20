package com.example.stockmanagermicroservice.dto;

public class ProcessInfoDTO {
    private String name;
    private double ramUsageMb;

    public ProcessInfoDTO() {}

    public ProcessInfoDTO(String name, double ramUsageMb) {
        this.name = name;
        this.ramUsageMb = ramUsageMb;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public double getRamUsageMb() {
        return ramUsageMb;
    }

    public void setRamUsageMb(double ramUsageMb) {
        this.ramUsageMb = ramUsageMb;
    }
}
