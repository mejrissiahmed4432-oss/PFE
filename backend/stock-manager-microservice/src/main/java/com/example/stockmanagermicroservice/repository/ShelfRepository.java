package com.example.stockmanagermicroservice.repository;

import com.example.stockmanagermicroservice.model.Shelf;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ShelfRepository extends MongoRepository<Shelf, String> {
    List<Shelf> findByEquipmentType(String equipmentType);
}
