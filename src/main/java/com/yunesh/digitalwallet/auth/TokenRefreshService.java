package com.yunesh.digitalwallet.auth;

import com.yunesh.digitalwallet.config.JwtConfig;
import com.yunesh.digitalwallet.exception.InvalidTokenException;
import com.yunesh.digitalwallet.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.Base64;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TokenRefreshService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtService jwtService;
    private final JwtConfig jwtConfig;

    @Transactional
    public LoginResponse refresh(String rawToken) {
        String hash = hashToken(rawToken);

        RefreshToken stored = refreshTokenRepository.findByTokenHash(hash)
                .orElseThrow(() -> new InvalidTokenException(
                        "Refresh token not found"));

        if (stored.isRevoked()) {
            throw new InvalidTokenException("Refresh token has been revoked");
        }

        if (stored.getExpiresAt().isBefore(Instant.now())) {
            throw new InvalidTokenException("Refresh token has expired");
        }

        User user = stored.getUser();

        // rotation — delete old, issue new
        refreshTokenRepository.delete(stored);

        String newRawToken = UUID.randomUUID().toString();
        String newHash = hashToken(newRawToken);

        RefreshToken newToken = RefreshToken.builder()
                .user(user)
                .tokenHash(newHash)
                .expiresAt(Instant.now().plusMillis(jwtConfig.getRefreshTokenExpiry()))
                .revoked(false)
                .build();

        refreshTokenRepository.save(newToken);

        String accessToken = jwtService.generateAccessToken(
                user.getId(), user.getEmail(), user.getRole().name());

        return new LoginResponse(accessToken, newRawToken,
                user.getId(), user.getEmail(), user.getRole().name());
    }

    @Transactional
    public void revoke(String rawToken) {
        String hash = hashToken(rawToken);

        RefreshToken stored = refreshTokenRepository.findByTokenHash(hash)
                .orElseThrow(() -> new InvalidTokenException(
                        "Refresh token not found"));

        stored.setRevoked(true);
        refreshTokenRepository.save(stored);
    }

    public static String hashToken(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(
                    rawToken.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hashBytes);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 not available", e);
        }
    }
}