package com.example.stockmanagermicroservice.repository;

import com.example.stockmanagermicroservice.model.Department;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DepartmentRepository extends MongoRepository<Department, String> {
}
