package com.example.stockmanagermicroservice.repository;

import com.example.stockmanagermicroservice.model.EquipmentMetricsSnapshot;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface EquipmentMetricsSnapshotRepository extends MongoRepository<EquipmentMetricsSnapshot, String> {
    List<EquipmentMetricsSnapshot> findByEquipmentIdAndTimestampAfterOrderByTimestampAsc(String equipmentId, LocalDateTime timestamp);
}
