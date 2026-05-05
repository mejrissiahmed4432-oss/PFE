package com.example.stockmanagermicroservice.repository;

import com.example.stockmanagermicroservice.model.Application;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ApplicationRepository extends MongoRepository<Application, String> {
}
