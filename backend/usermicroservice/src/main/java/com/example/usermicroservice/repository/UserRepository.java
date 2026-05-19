package com.example.usermicroservice.repository;

import com.example.usermicroservice.model.User;
import com.example.usermicroservice.model.Role;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.Optional;
import java.util.List;

public interface UserRepository extends MongoRepository<User, String> {
    Optional<User> findByEmail(String email);
    Optional<User> findByResetToken(String token);
    List<User> findByRole(Role role);
}
