package com.example.stockmanagermicroservice.repository;

import com.example.stockmanagermicroservice.model.LicensePool;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LicensePoolRepository extends MongoRepository<LicensePool, String> {
    List<LicensePool> findBySoftwareId(String softwareId);
}
