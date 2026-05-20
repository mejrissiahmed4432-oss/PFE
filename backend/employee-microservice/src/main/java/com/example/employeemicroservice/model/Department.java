package com.example.employeemicroservice.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.LocalDate;

@Document(collection = "departments")
public class Department {

    @Id
    private String id;

    private String name;
    private String description;
    private String headOfDepartment;

    @com.fasterxml.jackson.annotation.JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate createdAt;

    public Department() {
        this.createdAt = LocalDate.now();
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
}
