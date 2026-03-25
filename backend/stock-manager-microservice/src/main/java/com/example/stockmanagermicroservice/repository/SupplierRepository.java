package com.example.stockmanagermicroservice.repository;

import com.example.stockmanagermicroservice.model.Supplier;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SupplierRepository extends MongoRepository<Supplier, String> {
}
