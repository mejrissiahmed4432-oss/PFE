package com.example.aiservice.repository;

import com.example.aiservice.model.PendingAuditLog;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PendingAuditLogRepository extends MongoRepository<PendingAuditLog, String> {
}
