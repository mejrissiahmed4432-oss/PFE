package com.example.itmanagermicroservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableDiscoveryClient
@EnableFeignClients(defaultConfiguration = com.example.itmanagermicroservice.config.FeignConfig.class)

public class ItManagerMicroserviceApplication {
    public static void main(String[] args) {
        SpringApplication.run(ItManagerMicroserviceApplication.class, args);
    }
}
