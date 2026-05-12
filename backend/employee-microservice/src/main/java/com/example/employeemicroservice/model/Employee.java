package com.example.employeemicroservice.model;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.LocalDate;

@Document(collection = "employees")
public class Employee {
    @Id
    private String id;

    @NotBlank(message = "First name is required")
    private String firstName;

    @NotBlank(message = "Last name is required")
    private String lastName;

    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    private String email;

    private String phone;

    @NotBlank(message = "Job title is required")
    private String jobTitle;

    @NotBlank(message = "Department is required")
    private String department;

    private String cin;

    @com.fasterxml.jackson.annotation.JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate hireDate;

    // Status can be: Active, On Leave, Terminated, etc.
    private String employmentStatus;

    // Optional link to UserMicroservice account
    private String userId;

    public Employee() {
        this.employmentStatus = "Active";
        this.hireDate = LocalDate.now();
    }

    public Employee(String firstName, String lastName, String email, String phone, String jobTitle, String department,
            String cin) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.phone = phone;
        this.jobTitle = jobTitle;
        this.department = department;
        this.cin = cin;
        this.employmentStatus = "Active";
        this.hireDate = LocalDate.now();
    }

    // Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getJobTitle() {
        return jobTitle;
    }

    public void setJobTitle(String jobTitle) {
        this.jobTitle = jobTitle;
    }

    public String getDepartment() {
        return department;
    }

    public void setDepartment(String department) {
        this.department = department;
    }

    public String getCin() {
        return cin;
    }

    public void setCin(String cin) {
        this.cin = cin;
    }

    public LocalDate getHireDate() {
        return hireDate;
    }

    public void setHireDate(LocalDate hireDate) {
        this.hireDate = hireDate;
    }

    public String getEmploymentStatus() {
        return employmentStatus;
    }

    public void setEmploymentStatus(String employmentStatus) {
        this.employmentStatus = employmentStatus;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }
}
