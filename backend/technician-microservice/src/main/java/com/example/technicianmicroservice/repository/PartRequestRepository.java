package com.example.technicianmicroservice.repository;

import com.example.technicianmicroservice.model.PartRequest;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PartRequestRepository extends MongoRepository<PartRequest, String> {
    List<PartRequest> findByRequesterId(String requesterId);
}
