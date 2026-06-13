package com.example.stockmanagermicroservice.repository;

import com.example.stockmanagermicroservice.model.PendingAuditLog;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PendingAuditLogRepository extends MongoRepository<PendingAuditLog, String> {
}
