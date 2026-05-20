package com.example.apigateway.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.Key;

@Component
public class JwtUtil {

    // Must match the secret key used in usermicroservice
    private final String SECRET_KEY = "mySecretKeyForJwtAuthenticationWhichShouldBeLongEnoughAndSecure!";
    private final Key key = Keys.hmacShaKeyFor(SECRET_KEY.getBytes(StandardCharsets.UTF_8));

    public Claims getClaims(final String token) {
        return Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    public void validateToken(final String token) {
        Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token);
    }
}
