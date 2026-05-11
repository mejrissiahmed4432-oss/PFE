package com.example.stockmanagermicroservice.procurement.repository;

import com.example.stockmanagermicroservice.procurement.model.RFQToken;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RFQTokenRepository extends MongoRepository<RFQToken, String> {
    Optional<RFQToken> findByToken(String token);
}
