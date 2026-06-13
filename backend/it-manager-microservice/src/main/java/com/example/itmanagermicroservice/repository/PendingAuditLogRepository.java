package com.example.itmanagermicroservice.repository;

import com.example.itmanagermicroservice.model.PendingAuditLog;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PendingAuditLogRepository extends MongoRepository<PendingAuditLog, String> {
}
