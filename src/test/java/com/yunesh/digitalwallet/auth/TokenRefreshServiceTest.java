package com.yunesh.digitalwallet.auth;

import com.yunesh.digitalwallet.config.JwtConfig;
import com.yunesh.digitalwallet.exception.InvalidTokenException;
import com.yunesh.digitalwallet.user.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TokenRefreshServiceTest {

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private JwtService jwtService;

    @Mock
    private JwtConfig jwtConfig;

    @InjectMocks
    private TokenRefreshService tokenRefreshService;

    private String rawToken;
    private String tokenHash;
    private User user;
    private RefreshToken refreshToken;

    @BeforeEach
    void setUp() {
        rawToken = UUID.randomUUID().toString();
        tokenHash = TokenRefreshService.hashToken(rawToken);

        user = new User();
        user.setId(UUID.randomUUID());
        user.setEmail("test@example.com");
        user.setRole(com.yunesh.digitalwallet.user.UserRole.USER); // add this line

        refreshToken = new RefreshToken();
        refreshToken.setTokenHash(tokenHash);
        refreshToken.setUser(user);
        refreshToken.setRevoked(false);
        refreshToken.setExpiresAt(Instant.now().plusSeconds(3600));
    }

    @Test
    void refresh_validToken_returnsNewTokenPair() {
        when(refreshTokenRepository.findByTokenHash(tokenHash))
                .thenReturn(Optional.of(refreshToken));
        when(jwtConfig.getRefreshTokenExpiry()).thenReturn(604800000L);
        when(jwtService.generateAccessToken(any(), any(), any()))
                .thenReturn("new-access-token");
        when(refreshTokenRepository.save(any()))
                .thenAnswer(i -> i.getArgument(0));

        LoginResponse response = tokenRefreshService.refresh(rawToken);

        assertThat(response.accessToken()).isEqualTo("new-access-token");
        verify(refreshTokenRepository, times(1)).delete(refreshToken);
        verify(refreshTokenRepository, times(1)).save(any());
    }

    @Test
    void refresh_revokedToken_throwsInvalidTokenException() {
        refreshToken.setRevoked(true);
        when(refreshTokenRepository.findByTokenHash(tokenHash))
                .thenReturn(Optional.of(refreshToken));

        assertThatThrownBy(() -> tokenRefreshService.refresh(rawToken))
                .isInstanceOf(InvalidTokenException.class)
                .hasMessageContaining("revoked");
    }

    @Test
    void refresh_expiredToken_throwsInvalidTokenException() {
        refreshToken.setExpiresAt(Instant.now().minusSeconds(3600));
        when(refreshTokenRepository.findByTokenHash(tokenHash))
                .thenReturn(Optional.of(refreshToken));

        assertThatThrownBy(() -> tokenRefreshService.refresh(rawToken))
                .isInstanceOf(InvalidTokenException.class)
                .hasMessageContaining("expired");
    }

    @Test
    void refresh_tokenNotFound_throwsInvalidTokenException() {
        when(refreshTokenRepository.findByTokenHash(tokenHash))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> tokenRefreshService.refresh(rawToken))
                .isInstanceOf(InvalidTokenException.class)
                .hasMessageContaining("not found");
    }

    @Test
    void revoke_validToken_setsRevokedTrue() {
        when(refreshTokenRepository.findByTokenHash(tokenHash))
                .thenReturn(Optional.of(refreshToken));
        when(refreshTokenRepository.save(any()))
                .thenAnswer(i -> i.getArgument(0));

        tokenRefreshService.revoke(rawToken);

        assertThat(refreshToken.isRevoked()).isTrue();
        verify(refreshTokenRepository, times(1)).save(refreshToken);
    }
}