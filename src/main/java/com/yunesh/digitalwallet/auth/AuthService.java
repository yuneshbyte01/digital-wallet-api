package com.yunesh.digitalwallet.auth;

import com.yunesh.digitalwallet.audit.AuditAction;
import com.yunesh.digitalwallet.audit.AuditService;
import com.yunesh.digitalwallet.config.JwtConfig;
import com.yunesh.digitalwallet.exception.EmailAlreadyExistsException;
import com.yunesh.digitalwallet.exception.ResourceNotFoundException;
import com.yunesh.digitalwallet.user.User;
import com.yunesh.digitalwallet.user.UserRepository;
import com.yunesh.digitalwallet.wallet.Wallet;
import com.yunesh.digitalwallet.wallet.WalletRepository;
import com.yunesh.digitalwallet.wallet.WalletStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService implements UserDetailsService {

    private final UserRepository userRepository;
    private final BCryptPasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtConfig jwtConfig;
    private final TokenRefreshService tokenRefreshService;
    private final WalletRepository walletRepository;
    private final AuditService auditService;

    @Transactional
    public RegisterResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new EmailAlreadyExistsException(
                    "Email already registered: " + request.email()
            );
        }

        User user = User.builder()
                .fullName(request.fullName())
                .email(request.email())
                .passwordHash(passwordEncoder.encode(request.password()))
                .phone(request.phone())
                .build();

        User saved = userRepository.save(user);

        Wallet wallet = Wallet.builder()
                .user(saved)
                .currency("NPR")
                .status(WalletStatus.ACTIVE)
                .build();

        walletRepository.save(wallet);

        return new RegisterResponse(
                saved.getId(),
                saved.getFullName(),
                saved.getEmail(),
                saved.getPhone(),
                saved.getRole().name(),
                saved.getKycStatus().name()
        );
    }

    @Transactional
    public LoginResponse login(LoginRequest request, String ipAddress, String userAgent) {
        User user = userRepository.findByEmail(request.email()).orElse(null);

        if (user == null || !passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            UUID userId = user != null ? user.getId() : null;
            auditService.logAction(AuditAction.USER_LOGIN_FAILED, userId, ipAddress, userAgent,
                    Map.of("email", request.email()));
            throw new BadCredentialsException("Invalid email or password");
        }

        String accessToken = jwtService.generateAccessToken(
                user.getId(), user.getEmail(), user.getRole().name());

        String rawRefreshToken = UUID.randomUUID().toString();
        String tokenHash = TokenRefreshService.hashToken(rawRefreshToken);

        RefreshToken refreshToken = RefreshToken.builder()
                .user(user)
                .tokenHash(tokenHash)
                .expiresAt(Instant.now().plusMillis(jwtConfig.getRefreshTokenExpiry()))
                .revoked(false)
                .build();

        refreshTokenRepository.save(refreshToken);

        auditService.logAction(AuditAction.USER_LOGIN, user.getId(), ipAddress, userAgent, null);

        return new LoginResponse(accessToken, rawRefreshToken,
                user.getId(), user.getEmail(), user.getRole().name());
    }

    @Transactional
    public void logout(LogoutRequest request) {
        tokenRefreshService.revoke(request.refreshToken());
    }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException(
                        "User not found: " + email));

        return new org.springframework.security.core.userdetails.User(
                user.getEmail(),
                user.getPasswordHash(),
                List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()))
        );
    }
}