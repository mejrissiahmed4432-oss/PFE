package com.example.stockmanagermicroservice.procurement.repository;

import com.example.stockmanagermicroservice.procurement.model.EquipmentSpecification;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EquipmentSpecificationRepository extends MongoRepository<EquipmentSpecification, String> {
    List<EquipmentSpecification> findByCatalogItemId(String catalogItemId);
}
