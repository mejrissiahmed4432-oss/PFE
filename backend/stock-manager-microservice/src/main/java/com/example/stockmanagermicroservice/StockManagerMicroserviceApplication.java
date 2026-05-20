 package com.example.stockmanagermicroservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@EnableDiscoveryClient
@EnableFeignClients
public class StockManagerMicroserviceApplication {

    public static void main(String[] args) {
        SpringApplication.run(StockManagerMicroserviceApplication.class, args);
    }



    @Bean
    public org.springframework.boot.CommandLineRunner logEndpoints(org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping mapping) {
        return args -> {
            System.out.println("--- REGISTERED ENDPOINTS ---");
            mapping.getHandlerMethods().forEach((key, value) -> System.out.println("Mapped: " + key));
            System.out.println("----------------------------");
        };
    }
}
