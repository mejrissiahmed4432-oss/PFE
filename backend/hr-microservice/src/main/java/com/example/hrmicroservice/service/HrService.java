package com.example.hrmicroservice.service;

import com.example.hrmicroservice.client.EmployeeClient;
import com.example.hrmicroservice.dto.EmployeeDTO;
import com.example.hrmicroservice.dto.HrDashboardStats;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class HrService {

    @Autowired
    private EmployeeClient employeeClient;

    public HrDashboardStats getDashboardStats() {
        List<EmployeeDTO> employees = employeeClient.getAllEmployees();

        long total = employees.size();
        long active = employees.stream().filter(e -> "Active".equals(e.getEmploymentStatus())).count();
        long onLeave = employees.stream().filter(e -> "On Leave".equals(e.getEmploymentStatus())).count();
        long terminated = employees.stream().filter(e -> "Terminated".equals(e.getEmploymentStatus())).count();

        // New hires this month
        LocalDate firstOfMonth = LocalDate.now().withDayOfMonth(1);
        long newHires = employees.stream()
                .filter(e -> e.getHireDate() != null && !e.getHireDate().isBefore(firstOfMonth))
                .count();

        // Group by department
        Map<String, Long> byDepartment = employees.stream()
                .filter(e -> e.getDepartment() != null)
                .collect(Collectors.groupingBy(EmployeeDTO::getDepartment, Collectors.counting()));

        return new HrDashboardStats(total, active, onLeave, terminated, newHires, byDepartment);
    }
}
