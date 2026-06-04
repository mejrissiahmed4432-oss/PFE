package com.example.stockmanagermicroservice.repository;

import com.example.stockmanagermicroservice.model.EquipmentPrediction;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface EquipmentPredictionRepository extends MongoRepository<EquipmentPrediction, String> {
    Optional<EquipmentPrediction> findByEquipmentId(String equipmentId);
}
