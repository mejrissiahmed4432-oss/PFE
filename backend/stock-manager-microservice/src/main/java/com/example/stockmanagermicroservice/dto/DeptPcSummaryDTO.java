package com.example.stockmanagermicroservice.dto;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class DeptPcSummaryDTO {

    private String departmentName;
    private int totalLaptops;
    private int onlineCount;
    private int offlineCount;
    private int notFoundCount;
    private List<LaptopStatusDTO> laptops;
    private List<LaptopStatusDTO> topDevicesByCpu;
    private List<LaptopStatusDTO> topDevicesByRam;

    public DeptPcSummaryDTO() {}

    public DeptPcSummaryDTO(String departmentName, List<LaptopStatusDTO> laptops) {
        this.departmentName = departmentName;
        this.laptops = laptops;
        this.totalLaptops = laptops.size();
        this.onlineCount = (int) laptops.stream().filter(l -> "UP".equals(l.getUpStatus())).count();
        this.offlineCount = (int) laptops.stream().filter(l -> "DOWN".equals(l.getUpStatus())).count();
        this.notFoundCount = (int) laptops.stream().filter(l -> "NOT_FOUND_YET".equals(l.getUpStatus())).count();
        
        this.topDevicesByCpu = laptops.stream()
                .filter(l -> "UP".equals(l.getUpStatus()))
                .sorted(Comparator.comparingDouble(LaptopStatusDTO::getCpuPercent).reversed())
                .limit(3)
                .collect(Collectors.toList());

        this.topDevicesByRam = laptops.stream()
                .filter(l -> "UP".equals(l.getUpStatus()))
                .sorted(Comparator.comparingDouble(LaptopStatusDTO::getRamPercent).reversed())
                .limit(3)
                .collect(Collectors.toList());
    }

    public String getDepartmentName() { return departmentName; }
    public void setDepartmentName(String departmentName) { this.departmentName = departmentName; }

    public int getTotalLaptops() { return totalLaptops; }
    public void setTotalLaptops(int totalLaptops) { this.totalLaptops = totalLaptops; }

    public int getOnlineCount() { return onlineCount; }
    public void setOnlineCount(int onlineCount) { this.onlineCount = onlineCount; }

    public int getOfflineCount() { return offlineCount; }
    public void setOfflineCount(int offlineCount) { this.offlineCount = offlineCount; }

    public int getNotFoundCount() { return notFoundCount; }
    public void setNotFoundCount(int notFoundCount) { this.notFoundCount = notFoundCount; }

    public List<LaptopStatusDTO> getLaptops() { return laptops; }
    public void setLaptops(List<LaptopStatusDTO> laptops) { this.laptops = laptops; }

    public List<LaptopStatusDTO> getTopDevicesByCpu() { return topDevicesByCpu; }
    public void setTopDevicesByCpu(List<LaptopStatusDTO> topDevicesByCpu) { this.topDevicesByCpu = topDevicesByCpu; }

    public List<LaptopStatusDTO> getTopDevicesByRam() { return topDevicesByRam; }
    public void setTopDevicesByRam(List<LaptopStatusDTO> topDevicesByRam) { this.topDevicesByRam = topDevicesByRam; }
}
