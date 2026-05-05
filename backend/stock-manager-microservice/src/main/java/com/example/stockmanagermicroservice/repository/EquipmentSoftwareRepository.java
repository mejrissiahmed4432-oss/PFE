package com.example.stockmanagermicroservice.repository;

import com.example.stockmanagermicroservice.model.EquipmentSoftware;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EquipmentSoftwareRepository extends MongoRepository<EquipmentSoftware, String> {
    List<EquipmentSoftware> findByEquipmentId(String equipmentId);
    List<EquipmentSoftware> findByOsId(String osId);
    List<EquipmentSoftware> findByEquipmentIdAndStatus(String equipmentId, String status);
}
