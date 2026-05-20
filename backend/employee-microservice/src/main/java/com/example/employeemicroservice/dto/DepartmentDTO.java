package com.example.employeemicroservice.dto;

import java.time.LocalDate;

public class DepartmentDTO {
    private String id;
    private String name;
    private String description;
    private String headOfDepartment;
    private LocalDate createdAt;
    private long employeeCount;

    public DepartmentDTO() {}

    public DepartmentDTO(String id, String name, String description, String headOfDepartment, LocalDate createdAt, long employeeCount) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.headOfDepartment = headOfDepartment;
        this.createdAt = createdAt;
        this.employeeCount = employeeCount;
    }

    // --- Getters & Setters ---

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getHeadOfDepartment() { return headOfDepartment; }
    public void setHeadOfDepartment(String headOfDepartment) { this.headOfDepartment = headOfDepartment; }

    public LocalDate getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDate createdAt) { this.createdAt = createdAt; }

    public long getEmployeeCount() { return employeeCount; }
    public void setEmployeeCount(long employeeCount) { this.employeeCount = employeeCount; }
}
