package com.example.stockmanagermicroservice.procurement.repository;

import com.example.stockmanagermicroservice.procurement.model.EquipmentRequest;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EquipmentRequestRepository extends MongoRepository<EquipmentRequest, String> {
    List<EquipmentRequest> findByCreatedByUserId(String userId);
    List<EquipmentRequest> findAllByOrderByCreatedAtDesc();
}
