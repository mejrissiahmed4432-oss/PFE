package com.example.technicianmicroservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.data.mongodb.config.EnableMongoAuditing;

@SpringBootApplication
@EnableDiscoveryClient
@EnableMongoAuditing
@EnableFeignClients
public class TechnicianMicroserviceApplication {

    public static void main(String[] args) {
        SpringApplication.run(TechnicianMicroserviceApplication.class, args);
    }
}
