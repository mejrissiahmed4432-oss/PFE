package com.example.itmanagermicroservice.client;

import com.example.itmanagermicroservice.dto.EmployeeDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

@FeignClient(name = "employee-microservice")
public interface EmployeeClient {
    @GetMapping("/api/employees")
    List<EmployeeDTO> getAllEmployees();
}
