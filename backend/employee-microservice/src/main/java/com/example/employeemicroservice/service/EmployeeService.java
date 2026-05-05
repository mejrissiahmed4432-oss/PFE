package com.example.employeemicroservice.service;

import com.example.employeemicroservice.model.Employee;
import com.example.employeemicroservice.repository.EmployeeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class EmployeeService {

    @Autowired
    private EmployeeRepository employeeRepository;

    public List<Employee> getAllEmployees() {
        return employeeRepository.findAll();
    }

    public Optional<Employee> getEmployeeById(String id) {
        return employeeRepository.findById(id);
    }

    public Employee createEmployee(Employee employee) {
        return employeeRepository.save(employee);
    }

    public Employee updateEmployee(String id, Employee updatedEmployee) {
        return employeeRepository.findById(id)
                .map(employee -> {
                    employee.setFirstName(updatedEmployee.getFirstName());
                    employee.setLastName(updatedEmployee.getLastName());
                    employee.setEmail(updatedEmployee.getEmail());
                    employee.setPhone(updatedEmployee.getPhone());
                    employee.setJobTitle(updatedEmployee.getJobTitle());
                    employee.setDepartment(updatedEmployee.getDepartment());
                    if (updatedEmployee.getHireDate() != null) {
                        employee.setHireDate(updatedEmployee.getHireDate());
                    }
                    if (updatedEmployee.getEmploymentStatus() != null) {
                        employee.setEmploymentStatus(updatedEmployee.getEmploymentStatus());
                    }
                    if (updatedEmployee.getUserId() != null) {
                        employee.setUserId(updatedEmployee.getUserId());
                    }
                    return employeeRepository.save(employee);
                })
                .orElseThrow(() -> new RuntimeException("Employee not found with id " + id));
    }

    public void deleteEmployee(String id) {
        employeeRepository.deleteById(id);
    }
}
