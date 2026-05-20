package com.example.employeemicroservice.service;

import com.example.employeemicroservice.dto.DepartmentDTO;
import com.example.employeemicroservice.model.Department;
import com.example.employeemicroservice.model.Employee;
import com.example.employeemicroservice.repository.DepartmentRepository;
import com.example.employeemicroservice.repository.EmployeeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class DepartmentService {

    @Autowired
    private DepartmentRepository departmentRepository;

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private MongoTemplate mongoTemplate;

    public List<DepartmentDTO> getAllDepartments() {
        return departmentRepository.findAll().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    public Optional<DepartmentDTO> getDepartmentById(String id) {
        return departmentRepository.findById(id).map(this::convertToDTO);
    }

    public DepartmentDTO createDepartment(Department department) {
        if (department.getCreatedAt() == null) {
            department.setCreatedAt(LocalDate.now());
        }
        Department saved = departmentRepository.save(department);
        return convertToDTO(saved);
    }

    public DepartmentDTO updateDepartment(String id, Department updatedDept) {
        return departmentRepository.findById(id).map(dept -> {
            String oldName = dept.getName();
            String newName = updatedDept.getName();

            dept.setName(newName);
            dept.setDescription(updatedDept.getDescription());
            dept.setHeadOfDepartment(updatedDept.getHeadOfDepartment());
            Department saved = departmentRepository.save(dept);

            if (oldName != null && !oldName.equals(newName)) {
                // Update all employees matching the old department name
                List<Employee> employeesToUpdate = employeeRepository.findByDepartment(oldName);
                if (employeesToUpdate != null && !employeesToUpdate.isEmpty()) {
                    for (Employee e : employeesToUpdate) {
                        e.setDepartment(newName);
                        employeeRepository.save(e);
                    }
                }

                // Update all equipment matching the old department name
                Query query = new Query(Criteria.where("department").is(oldName));
                Update update = new Update().set("department", newName);
                mongoTemplate.updateMulti(query, update, "equipment");
            }

            return convertToDTO(saved);
        }).orElseThrow(() -> new RuntimeException("Department not found with id " + id));
    }

    public void deleteDepartment(String id) {
        departmentRepository.deleteById(id);
    }

    private DepartmentDTO convertToDTO(Department dept) {
        long employeeCount = employeeRepository.countByDepartment(dept.getName());
        return new DepartmentDTO(
                dept.getId(),
                dept.getName(),
                dept.getDescription(),
                dept.getHeadOfDepartment(),
                dept.getCreatedAt(),
                employeeCount);
    }
}
