package com.example.usermicroservice.repository;

import com.example.usermicroservice.model.Alert;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.List;

public interface AlertRepository extends MongoRepository<Alert, String> {
    List<Alert> findByReadFalseOrderByCreatedAtDesc();
    List<Alert> findAllByOrderByCreatedAtDesc();
    boolean existsByCategoryAndRelatedIdAndType(String category, String relatedId, String type);

    // Specific user-targeted queries
    @org.springframework.data.mongodb.repository.Query("{ 'read': false, '$or': [ { 'recipientId': ?0 }, { 'targetRole': ?1 }, { 'recipientId': null, 'targetRole': null } ] }")
    List<Alert> findUnreadForUser(String recipientId, String targetRole);

    @org.springframework.data.mongodb.repository.Query("{ '$or': [ { 'recipientId': ?0 }, { 'targetRole': ?1 }, { 'recipientId': null, 'targetRole': null } ] }")
    List<Alert> findAllForUser(String recipientId, String targetRole);
}
