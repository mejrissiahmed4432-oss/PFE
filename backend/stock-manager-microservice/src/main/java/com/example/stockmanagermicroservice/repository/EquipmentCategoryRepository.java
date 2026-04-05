package com.example.stockmanagermicroservice.repository;

import com.example.stockmanagermicroservice.model.EquipmentCategory;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface EquipmentCategoryRepository extends MongoRepository<EquipmentCategory, String> {
    Optional<EquipmentCategory> findByNameIgnoreCase(String name);
}
