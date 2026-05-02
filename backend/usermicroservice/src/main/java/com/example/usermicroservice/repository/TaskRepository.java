package com.example.usermicroservice.repository;

import com.example.usermicroservice.model.Task;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TaskRepository extends MongoRepository<Task, String> {
    List<Task> findByStatus(String status);
    List<Task> findByUserId(String userId);
}
