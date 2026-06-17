package com.example.usermicroservice.repository;

import com.example.usermicroservice.model.PersonalRequest;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PersonalRequestRepository extends MongoRepository<PersonalRequest, String> {
    List<PersonalRequest> findByUserId(String userId);
    List<PersonalRequest> findByStatus(String status);
}
