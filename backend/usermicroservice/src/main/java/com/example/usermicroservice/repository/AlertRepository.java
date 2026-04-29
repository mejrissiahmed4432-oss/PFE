package com.example.usermicroservice.repository;

import com.example.usermicroservice.model.Alert;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.List;
import java.util.Optional;

public interface AlertRepository extends MongoRepository<Alert, String> {
    
    Optional<Alert> findByKeyAndStatus(String key, String status);
    
    List<Alert> findByStatusOrderByCreatedAtDesc(String status);
    List<Alert> findAllByOrderByCreatedAtDesc();

}
