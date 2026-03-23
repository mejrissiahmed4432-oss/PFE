package com.example.stockmanagermicroservice.repository;

import com.example.stockmanagermicroservice.model.Equipment;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface EquipmentRepository extends MongoRepository<Equipment, String> {
}
