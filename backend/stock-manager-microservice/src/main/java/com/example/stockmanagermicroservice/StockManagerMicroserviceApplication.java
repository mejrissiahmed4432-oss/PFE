 package com.example.stockmanagermicroservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.client.RestTemplate;

@SpringBootApplication
@EnableScheduling
@org.springframework.data.mongodb.config.EnableMongoAuditing
public class StockManagerMicroserviceApplication {

    public static void main(String[] args) {
        SpringApplication.run(StockManagerMicroserviceApplication.class, args);
    }

    @Bean
    @org.springframework.cloud.client.loadbalancer.LoadBalanced
    public RestTemplate restTemplate() {
        return new RestTemplate();
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
