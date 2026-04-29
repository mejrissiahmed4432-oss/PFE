package com.example.stockmanagermicroservice.repository;

import com.example.stockmanagermicroservice.model.EquipmentApplication;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface EquipmentApplicationRepository extends MongoRepository<EquipmentApplication, String> {
    List<EquipmentApplication> findByEquipmentId(String equipmentId);
    List<EquipmentApplication> findByApplicationId(String applicationId);
}
