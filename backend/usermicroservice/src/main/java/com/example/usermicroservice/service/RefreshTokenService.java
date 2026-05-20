package com.example.usermicroservice.service;

import com.example.usermicroservice.model.RefreshToken;
import com.example.usermicroservice.repository.RefreshTokenRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Service
public class RefreshTokenService {

    // Refresh token valid for 7 days
    private static final long REFRESH_TOKEN_EXPIRY_MS = 7L * 24 * 60 * 60 * 1000;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    public RefreshToken createRefreshToken(String userEmail) {
        // Delete any existing refresh token for this user (one token per user)
        refreshTokenRepository.deleteByUserEmail(userEmail);

        RefreshToken refreshToken = new RefreshToken(
            UUID.randomUUID().toString(),
            userEmail,
            Instant.now().plusMillis(REFRESH_TOKEN_EXPIRY_MS)
        );
        return refreshTokenRepository.save(refreshToken);
    }

    public Optional<RefreshToken> findByToken(String token) {
        return refreshTokenRepository.findByToken(token);
    }

    public boolean isExpired(RefreshToken token) {
        return token.getExpiryDate().isBefore(Instant.now());
    }

    public void deleteByUserEmail(String userEmail) {
        refreshTokenRepository.deleteByUserEmail(userEmail);
    }
}
