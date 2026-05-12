package com.example.itmanagermicroservice.dto;

import lombok.Data;
import java.time.LocalDate;

@Data
public class EmployeeDTO {
    private String id;
    private String firstName;
    private String lastName;
    private String email;
    private String phone;
    private String jobTitle;
    private String department;
    private LocalDate hireDate;
    private String employmentStatus;
    private String cin;
}
