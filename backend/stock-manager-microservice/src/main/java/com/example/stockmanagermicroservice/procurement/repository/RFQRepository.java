package com.example.stockmanagermicroservice.procurement.repository;

import com.example.stockmanagermicroservice.procurement.model.RFQ;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RFQRepository extends MongoRepository<RFQ, String> {
    Optional<RFQ> findByRequestId(String requestId);
    List<RFQ> findAllByOrderByCreatedAtDesc();
    List<RFQ> findByIdStartingWithIgnoreCase(String idPrefix);
}
