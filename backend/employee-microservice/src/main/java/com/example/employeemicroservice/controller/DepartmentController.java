package com.example.employeemicroservice.controller;

import com.example.employeemicroservice.config.BlockchainTraceable;
import com.example.employeemicroservice.dto.DepartmentDTO;
import com.example.employeemicroservice.model.Department;
import com.example.employeemicroservice.service.DepartmentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/departments")
public class DepartmentController {

    @Autowired
    private DepartmentService departmentService;

    @GetMapping
    public ResponseEntity<List<DepartmentDTO>> getAllDepartments() {
        return ResponseEntity.ok(departmentService.getAllDepartments());
    }

    @GetMapping("/{id}")
    public ResponseEntity<DepartmentDTO> getDepartmentById(@PathVariable String id) {
        return departmentService.getDepartmentById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @BlockchainTraceable(action = "Create Department")
    @PostMapping
    public ResponseEntity<DepartmentDTO> createDepartment(@RequestBody Department department) {
        DepartmentDTO created = departmentService.createDepartment(department);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @BlockchainTraceable(action = "Update Department")
    @PutMapping("/{id}")
    public ResponseEntity<DepartmentDTO> updateDepartment(@PathVariable String id,
                                                        @RequestBody Department department) {
        try {
            DepartmentDTO updated = departmentService.updateDepartment(id, department);
            return ResponseEntity.ok(updated);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @BlockchainTraceable(action = "Delete Department")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteDepartment(@PathVariable String id) {
        departmentService.deleteDepartment(id);
        return ResponseEntity.noContent().build();
    }
}
