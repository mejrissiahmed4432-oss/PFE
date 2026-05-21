package com.example.stockmanagermicroservice.repository;

import com.example.stockmanagermicroservice.model.Software;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SoftwareRepository extends MongoRepository<Software, String> {
    List<Software> findByVendor(String vendor);
    List<Software> findByType(String type);
}
