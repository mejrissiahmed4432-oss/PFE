package com.example.usermicroservice.repository;

import com.example.usermicroservice.model.Notification;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificationRepository extends MongoRepository<Notification, String> {
    List<Notification> findByReadFalseOrderByCreatedAtDesc();
    List<Notification> findAllByOrderByCreatedAtDesc();

    // Specific user-targeted queries
    @org.springframework.data.mongodb.repository.Query("{ 'read': false, '$or': [ { 'recipientId': ?0 }, { 'recipientId': null } ] }")
    List<Notification> findUnreadForUser(String recipientId);

    @org.springframework.data.mongodb.repository.Query("{ '$or': [ { 'recipientId': ?0 }, { 'recipientId': null } ] }")
    List<Notification> findAllForUser(String recipientId);
}
