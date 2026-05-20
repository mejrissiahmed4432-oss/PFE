package com.example.apigateway.filter;

import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.function.Predicate;

@Component
public class RouteValidator {

    public static final List<String> openApiEndpoints = List.of(
            "/api/users/login",
            "/api/users/register",
            "/api/users/refresh-token",  // Allow token refresh without a valid access token
            "/api/users/logout",          // Allow logout even with expired token
            "/api/users/forgot-password",
            "/api/users/reset-password",
            "/eureka",
            "/ws",
            "/ws/"
    );

    public Predicate<ServerHttpRequest> isSecured =
            request -> openApiEndpoints
                    .stream()
                    .noneMatch(uri -> request.getURI().getPath().contains(uri));
}
