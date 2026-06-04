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
            "/api/public/",               // Allow public procurement routes (supplier portal)
            "/eureka",
            "/ws",
            "/ws/"
    );

    public Predicate<ServerHttpRequest> isSecured =
            request -> {
                String path = request.getURI().getPath();
                // Allow specific procurement endpoints ending with /view or /download
                if (path.matches("^/api/procurement/responses/[a-zA-Z0-9]+/(view|download)$")) {
                    return false;
                }
                return openApiEndpoints.stream().noneMatch(path::contains);
            };
}
