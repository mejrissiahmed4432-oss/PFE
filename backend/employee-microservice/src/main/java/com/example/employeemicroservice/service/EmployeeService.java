package com.example.employeemicroservice.service;

import com.example.employeemicroservice.model.Employee;
import com.example.employeemicroservice.repository.EmployeeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class EmployeeService {

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private MongoTemplate mongoTemplate;

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

                    Employee savedEmployee = employeeRepository.save(employee);

                    // Sync common fields to the users collection
                    // Link via employeeId or email
                    Criteria criteria = new Criteria().orOperator(
                            Criteria.where("employeeId").is(savedEmployee.getId()),
                            Criteria.where("email").is(savedEmployee.getEmail())
                    );
                    if (savedEmployee.getUserId() != null && !savedEmployee.getUserId().isEmpty()) {
                        criteria = new Criteria().orOperator(
                                Criteria.where("employeeId").is(savedEmployee.getId()),
                                Criteria.where("email").is(savedEmployee.getEmail()),
                                Criteria.where("_id").is(savedEmployee.getUserId())
                        );
                    }

                    Query query = new Query(criteria);
                    Update update = new Update()
                            .set("firstName", savedEmployee.getFirstName())
                            .set("lastName", savedEmployee.getLastName())
                            .set("email", savedEmployee.getEmail())
                            .set("phoneNumber", savedEmployee.getPhone());
                    mongoTemplate.updateMulti(query, update, "users");

                    return savedEmployee;
                })
                .orElseThrow(() -> new RuntimeException("Employee not found with id " + id));
    }

    public void deleteEmployee(String id) {
        employeeRepository.deleteById(id);
    }
}
