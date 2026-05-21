package com.example.usermicroservice.repository;

import com.example.usermicroservice.model.Ticket;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TicketRepository extends MongoRepository<Ticket, String> {
    List<Ticket> findByUserId(String userId);
    List<Ticket> findByStatus(String status);
    long countByAssignedToAndStatusIn(String assignedTo, List<String> statuses);
    @org.springframework.data.mongodb.repository.Query("{ '$or': [ { 'assignedTo': ?0 }, { 'userId': ?1 } ] }")
    List<Ticket> findByAssignedToOrUserId(String assignedTo, String userId);
}
