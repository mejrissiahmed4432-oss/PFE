package com.example.stockmanagermicroservice.dto;

public class ProcessInfoDTO {
    private String name;
    private double ramUsageMb;
    private int pid;

    public ProcessInfoDTO() {}

    public ProcessInfoDTO(String name, double ramUsageMb, int pid) {
        this.name = name;
        this.ramUsageMb = ramUsageMb;
        this.pid = pid;
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

    public int getPid() {
        return pid;
    }

    public void setPid(int pid) {
        this.pid = pid;
    }
}
