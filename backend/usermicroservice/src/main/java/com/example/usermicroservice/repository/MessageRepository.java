package com.example.usermicroservice.repository;

import com.example.usermicroservice.model.Message;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface MessageRepository extends MongoRepository<Message, String> {
    List<Message> findBySenderIdAndReceiverIdOrderByTimestampAsc(String senderId, String receiverId);
    List<Message> findByReceiverIdAndStatusOrderByTimestampAsc(String receiverId, String status);
    
    // Find all messages involving this user for sorting conversation list
    List<Message> findBySenderIdOrReceiverIdOrderByTimestampDesc(String senderId, String receiverId);
    
    // Find history between two users (sender to receiver OR receiver to sender)
    // Custom find for logic
}
