package com.example.stockmanagermicroservice.dto;

import java.util.Map;
import java.util.List;

public class LaptopStatusDTO {

    private String equipmentId;
    private String equipmentName;
    private String serialNumber;
    private String brand;
    private String model;
    private String department;
    private String ip;
    private String upStatus;   // UP | DOWN | NOT_FOUND_YET
    private double cpuPercent;
    private double ramPercent;
    private double totalRam;
    private double freeRam;
    private String macAddress;
    private String os;
    private double diskPercent;
    private double totalDisk;
    private double freeDisk;
    private Map<String, Double> diskVolumes;
    private String networkSpeed;
    private double netInSpeed;
    private double netOutSpeed;
    private String lastSeen;
    private java.util.List<ProcessInfoDTO> topProcesses;
    private String uptime;
    private double temperature;
    private int totalProcesses;

    public LaptopStatusDTO() {}

    public LaptopStatusDTO(String equipmentId, String equipmentName, String serialNumber,
                            String brand, String model, String department,
                            String ip, String upStatus) {
        this.equipmentId = equipmentId;
        this.equipmentName = equipmentName;
        this.serialNumber = serialNumber;
        this.brand = brand;
        this.model = model;
        this.department = department;
        this.ip = ip;
        this.upStatus = upStatus;
    }

    // --- Getters & Setters ---

    public String getEquipmentId() { return equipmentId; }
    public void setEquipmentId(String equipmentId) { this.equipmentId = equipmentId; }

    public String getEquipmentName() { return equipmentName; }
    public void setEquipmentName(String equipmentName) { this.equipmentName = equipmentName; }

    public String getSerialNumber() { return serialNumber; }
    public void setSerialNumber(String serialNumber) { this.serialNumber = serialNumber; }

    public String getBrand() { return brand; }
    public void setBrand(String brand) { this.brand = brand; }

    public String getModel() { return model; }
    public void setModel(String model) { this.model = model; }

    public String getDepartment() { return department; }
    public void setDepartment(String department) { this.department = department; }

    public String getIp() { return ip; }
    public void setIp(String ip) { this.ip = ip; }

    public String getUpStatus() { return upStatus; }
    public void setUpStatus(String upStatus) { this.upStatus = upStatus; }

    public double getCpuPercent() { return cpuPercent; }
    public void setCpuPercent(double cpuPercent) { this.cpuPercent = cpuPercent; }

    public double getRamPercent() { return ramPercent; }
    public void setRamPercent(double ramPercent) { this.ramPercent = ramPercent; }

    public String getMacAddress() { return macAddress; }
    public void setMacAddress(String macAddress) { this.macAddress = macAddress; }

    public String getOs() { return os; }
    public void setOs(String os) { this.os = os; }

    public double getDiskPercent() { return diskPercent; }
    public void setDiskPercent(double diskPercent) { this.diskPercent = diskPercent; }

    public String getNetworkSpeed() { return networkSpeed; }
    public void setNetworkSpeed(String networkSpeed) { this.networkSpeed = networkSpeed; }

    public String getLastSeen() { return lastSeen; }
    public void setLastSeen(String lastSeen) { this.lastSeen = lastSeen; }

    public java.util.Map<String, Double> getDiskVolumes() { return diskVolumes; }
    public void setDiskVolumes(java.util.Map<String, Double> diskVolumes) { this.diskVolumes = diskVolumes; }

    public double getNetInSpeed() { return netInSpeed; }
    public void setNetInSpeed(double netInSpeed) { this.netInSpeed = netInSpeed; }

    public double getNetOutSpeed() { return netOutSpeed; }
    public void setNetOutSpeed(double netOutSpeed) { this.netOutSpeed = netOutSpeed; }

    public java.util.List<ProcessInfoDTO> getTopProcesses() { return topProcesses; }
    public void setTopProcesses(java.util.List<ProcessInfoDTO> topProcesses) { this.topProcesses = topProcesses; }

    public String getUptime() { return uptime; }
    public void setUptime(String uptime) { this.uptime = uptime; }

    public double getTotalRam() { return totalRam; }
    public void setTotalRam(double totalRam) { this.totalRam = totalRam; }

    public double getFreeRam() { return freeRam; }
    public void setFreeRam(double freeRam) { this.freeRam = freeRam; }

    public double getTotalDisk() { return totalDisk; }
    public void setTotalDisk(double totalDisk) { this.totalDisk = totalDisk; }

    public double getFreeDisk() { return freeDisk; }
    public void setFreeDisk(double freeDisk) { this.freeDisk = freeDisk; }

    public double getTemperature() { return temperature; }
    public void setTemperature(double temperature) { this.temperature = temperature; }

    public int getTotalProcesses() { return totalProcesses; }
    public void setTotalProcesses(int totalProcesses) { this.totalProcesses = totalProcesses; }
}
