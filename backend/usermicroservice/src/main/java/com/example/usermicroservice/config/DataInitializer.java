package com.example.usermicroservice.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.example.usermicroservice.model.Role;
import com.example.usermicroservice.model.User;
import com.example.usermicroservice.repository.UserRepository;

@Configuration
public class DataInitializer {

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Bean
    CommandLineRunner initDatabase(UserRepository repository) {
        return args -> {
            String adminEmail = "mejrissiahmed4432@gmail.com";
            if (repository.findByEmail(adminEmail).isEmpty()) {
                User admin = new User(
                    "Ahmed", 
                    "Mejrissi", 
                    adminEmail, 
                    passwordEncoder.encode("1234656Aa"), 
                    Role.ADMIN
                );
                repository.save(admin);
                System.out.println("Admin user created: " + adminEmail);
            } else {
                System.out.println("Admin user already exists.");
            }
        };
    }
}
