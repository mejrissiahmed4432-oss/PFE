package com.example.stockmanagermicroservice.repository;

import com.example.stockmanagermicroservice.model.Alert;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.List;

public interface AlertRepository extends MongoRepository<Alert, String> {
    List<Alert> findByReadFalseOrderByCreatedAtDesc();
    List<Alert> findAllByOrderByCreatedAtDesc();
}
