package com.example.technicianmicroservice.config;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Configuration
public class FeignClientInterceptor implements RequestInterceptor {

    @Override
    public void apply(RequestTemplate requestTemplate) {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        
        if (attributes != null) {
            HttpServletRequest request = attributes.getRequest();
            
            // Forward the identity headers if they exist
            String cin = request.getHeader("X-User-CIN");
            if (cin != null && !cin.isEmpty()) {
                requestTemplate.header("X-User-CIN", cin);
            }
            
            String name = request.getHeader("X-User-Name");
            if (name != null && !name.isEmpty()) {
                requestTemplate.header("X-User-Name", name);
            }
            
            String role = request.getHeader("X-User-Role");
            if (role != null && !role.isEmpty()) {
                requestTemplate.header("X-User-Role", role);
            }
            
            String email = request.getHeader("X-User-Email");
            if (email != null && !email.isEmpty()) {
                requestTemplate.header("X-User-Email", email);
            }
            
            // Optionally forward Authorization header if JWT token propagation is needed
            String authHeader = request.getHeader("Authorization");
            if (authHeader != null && !authHeader.isEmpty()) {
                requestTemplate.header("Authorization", authHeader);
            }
        }
    }
}
