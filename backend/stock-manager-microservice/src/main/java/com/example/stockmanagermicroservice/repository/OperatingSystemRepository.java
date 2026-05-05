package com.example.stockmanagermicroservice.repository;

import com.example.stockmanagermicroservice.model.OperatingSystem;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OperatingSystemRepository extends MongoRepository<OperatingSystem, String> {
    List<OperatingSystem> findByResourceId(String resourceId);
    List<OperatingSystem> findByStatus(String status);
}
