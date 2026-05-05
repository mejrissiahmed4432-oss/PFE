package com.example.stockmanagermicroservice.repository;

import com.example.stockmanagermicroservice.model.Resource;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ResourceRepository extends MongoRepository<Resource, String> {
    List<Resource> findByCategory(String category);
}
