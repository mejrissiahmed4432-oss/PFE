package com.example.employeemicroservice.repository;

import com.example.employeemicroservice.model.Department;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface DepartmentRepository extends MongoRepository<Department, String> {
    Optional<Department> findByName(String name);
    boolean existsByName(String name);
}
