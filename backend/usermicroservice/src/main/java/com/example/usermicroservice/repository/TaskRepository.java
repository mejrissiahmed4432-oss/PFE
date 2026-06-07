package com.example.usermicroservice.repository;

import com.example.usermicroservice.model.Task;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TaskRepository extends MongoRepository<Task, String> {
    List<Task> findByStatus(String status);
    List<Task> findByUserId(String userId);
    long countByUserIdAndStatusIn(String userId, List<String> statuses);
    long countByAssignedToAndStatusIn(String assignedTo, List<String> statuses);
    // Multi-user assignment queries
    List<Task> findByAssignedUserIdsContaining(String userId);
    List<Task> findByAssignedByUserId(String assignedByUserId);
}
