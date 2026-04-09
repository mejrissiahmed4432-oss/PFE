package com.example.stockmanagermicroservice.repository;

import com.example.stockmanagermicroservice.model.Equipment;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EquipmentRepository extends MongoRepository<Equipment, String> {
    List<Equipment> findBySupplier(String supplier);
    List<Equipment> findBySupplierId(String supplierId);
    List<Equipment> findByShelfId(String shelfId);
    List<Equipment> findByCategory(String category);
    boolean existsByCategory(String category);
    boolean existsByTypeIgnoreCase(String type);
    boolean existsBySerialNumber(String serialNumber);
    boolean existsBySerialNumberAndIdNot(String serialNumber, String id);
    boolean existsByShelfId(String shelfId);
    boolean existsBySupplierId(String supplierId);
    List<Equipment> findByTypeIgnoreCase(String type);
}
