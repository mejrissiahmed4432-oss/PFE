package com.example.usermicroservice.repository;

import com.example.usermicroservice.model.PendingAuditLog;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PendingAuditLogRepository extends MongoRepository<PendingAuditLog, String> {
}
