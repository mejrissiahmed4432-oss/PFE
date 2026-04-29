package com.example.aiservice.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;

@Configuration
public class AppConfig {

    @Value("${app.stock-service.url}")
    private String stockServiceUrl;

    @Value("${app.technician-service.url}")
    private String technicianServiceUrl;

    @Value("${app.user-service.url}")
    private String userServiceUrl;

    @Bean("stockWebClient")
    public WebClient stockWebClient() {
        return WebClient.builder()
                .baseUrl(stockServiceUrl)
                .codecs(c -> c.defaultCodecs().maxInMemorySize(10 * 1024 * 1024))
                .build();
    }

    @Bean("technicianWebClient")
    public WebClient technicianWebClient() {
        return WebClient.builder()
                .baseUrl(technicianServiceUrl)
                .codecs(c -> c.defaultCodecs().maxInMemorySize(10 * 1024 * 1024))
                .build();
    }

    @Bean("userWebClient")
    public WebClient userWebClient() {
        return WebClient.builder()
                .baseUrl(userServiceUrl)
                .codecs(c -> c.defaultCodecs().maxInMemorySize(10 * 1024 * 1024))
                .build();
    }
}
