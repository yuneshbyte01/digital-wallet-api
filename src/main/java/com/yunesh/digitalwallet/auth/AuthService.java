package com.yunesh.digitalwallet.auth;

import com.yunesh.digitalwallet.audit.AuditAction;
import com.yunesh.digitalwallet.audit.AuditService;
import com.yunesh.digitalwallet.common.AppConstants;
import com.yunesh.digitalwallet.config.EncoderConfig;
import com.yunesh.digitalwallet.config.JwtConfig;
import com.yunesh.digitalwallet.exception.AccountLockedException;
import com.yunesh.digitalwallet.exception.EmailAlreadyExistsException;
import com.yunesh.digitalwallet.exception.PhoneAlreadyExistsException;
import com.yunesh.digitalwallet.user.AccountStatus;
import com.yunesh.digitalwallet.user.User;
import com.yunesh.digitalwallet.user.UserRepository;
import com.yunesh.digitalwallet.wallet.Wallet;
import com.yunesh.digitalwallet.wallet.WalletRepository;
import com.yunesh.digitalwallet.wallet.WalletStatus;
import jakarta.annotation.Nonnull;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService implements UserDetailsService {

    private final UserRepository userRepository;
    private final EncoderConfig encoderConfig;
    private final JwtService jwtService;
    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtConfig jwtConfig;
    private final TokenRefreshService tokenRefreshService;
    private final WalletRepository walletRepository;
    private final AuditService auditService;
    private final AuthMapper authMapper;

    @Transactional
    public RegisterResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new EmailAlreadyExistsException(
                    "Email already registered: " + request.email());
        }

        if (userRepository.existsByPhone(request.phone())) {
            throw new PhoneAlreadyExistsException(
                    "Phone already registered: " + request.phone());
        }

        User user = User.builder()
                .fullName(request.fullName())
                .email(request.email())
                .passwordHash(encoderConfig.getPasswordEncoder()
                        .encode(request.password()))
                .phone(request.phone())
                .gender(request.gender())
                .pinHash(encoderConfig.getPinEncoder()
                        .encode(request.pin()))
                .build();

        User saved = userRepository.save(user);

        Wallet wallet = Wallet.builder()
                .user(saved)
                .currency("NPR")
                .status(WalletStatus.ACTIVE)
                .build();

        walletRepository.save(wallet);

        auditService.logAction(
                AuditAction.USER_REGISTERED,
                saved.getId(),
                null, null, null);

        return authMapper.toRegisterResponse(saved);
    }

    @Transactional(noRollbackFor = {
            BadCredentialsException.class,
            AccountLockedException.class
    })
    public LoginResponse login(LoginRequest request,
                               String ipAddress,
                               String userAgent) {

        String identifier = request.identifier();
        boolean isEmail = identifier.contains("@");
        User user;

        if (isEmail) {
            if (request.password() == null || request.password().isBlank()) {
                throw new BadCredentialsException(
                        "Password is required for email login");
            }

            user = userRepository.findByEmail(identifier).orElse(null);

            if (user == null) {
                auditService.logAction(AuditAction.USER_LOGIN_FAILED,
                        null, ipAddress, userAgent,
                        "{\"identifier\":\"" + identifier + "\"}");
                throw new BadCredentialsException("Invalid email or password");
            }

            checkAndHandleLock(user);

            if (!encoderConfig.getPasswordEncoder().matches(
                    request.password(), user.getPasswordHash())) {
                handleFailedAttempt(user, ipAddress, userAgent, identifier);
                throw new BadCredentialsException(
                        user.getStatus() == AccountStatus.LOCKED
                                ? "Account locked for "
                                + AppConstants.Security.LOCK_DURATION_MINUTES
                                + " minutes due to too many failed attempts"
                                : "Invalid email or password. Attempts remaining: "
                                + (AppConstants.Security.MAX_PIN_ATTEMPTS
                                - user.getFailedLoginAttempts()));
            }

        } else {
            if (request.pin() == null || request.pin().isBlank()) {
                throw new BadCredentialsException(
                        "PIN is required for phone login");
            }

            user = userRepository.findByPhone(identifier).orElse(null);

            if (user == null) {
                auditService.logAction(AuditAction.USER_LOGIN_FAILED,
                        null, ipAddress, userAgent,
                        "{\"identifier\":\"" + identifier + "\"}");
                throw new BadCredentialsException("Invalid phone or PIN");
            }

            checkAndHandleLock(user);

            if (user.getPinHash() == null) {
                throw new BadCredentialsException(
                        "PIN not set for this account");
            }

            if (!encoderConfig.getPinEncoder().matches(
                    request.pin(), user.getPinHash())) {
                handleFailedAttempt(user, ipAddress, userAgent, identifier);
                throw new BadCredentialsException(
                        user.getStatus() == AccountStatus.LOCKED
                                ? "Account locked for "
                                + AppConstants.Security.LOCK_DURATION_MINUTES
                                + " minutes due to too many failed attempts"
                                : "Invalid phone or PIN. Attempts remaining: "
                                + (AppConstants.Security.MAX_PIN_ATTEMPTS
                                - user.getFailedLoginAttempts()));
            }
        }

        // successful login — reset attempts and update lastLoginAt
        user.setFailedLoginAttempts(0);
        user.setAccountLockedUntil(null);
        user.setLastLoginAt(Instant.now());
        userRepository.save(user);

        String accessToken = jwtService.generateAccessToken(
                user.getId(), user.getEmail(), user.getRole().name());

        String rawRefreshToken = UUID.randomUUID().toString();
        String tokenHash = TokenRefreshService.hashToken(rawRefreshToken);

        RefreshToken refreshToken = RefreshToken.builder()
                .user(user)
                .tokenHash(tokenHash)
                .expiresAt(Instant.now().plusMillis(
                        jwtConfig.getRefreshTokenExpiry()))
                .revoked(false)
                .build();

        refreshTokenRepository.save(refreshToken);

        auditService.logAction(AuditAction.USER_LOGIN,
                user.getId(), ipAddress, userAgent, null);

        return authMapper.toLoginResponse(user, accessToken, rawRefreshToken);
    }

    @Transactional
    public void logout(LogoutRequest request) {
        tokenRefreshService.revoke(request.refreshToken());
    }

    @Nonnull
    @Override
    public UserDetails loadUserByUsername(@Nonnull String email)
            throws UsernameNotFoundException {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException(
                        "User not found: " + email));

        return new org.springframework.security.core.userdetails.User(
                user.getEmail(),
                user.getPasswordHash(),
                List.of(new SimpleGrantedAuthority(
                        "ROLE_" + user.getRole().name()))
        );
    }

    // check if locked — auto unlock if the temporary lock expired
    private void checkAndHandleLock(User user) {
        if (user.getStatus() != AccountStatus.LOCKED) return;

        if (user.getAccountLockedUntil() != null
                && Instant.now().isAfter(user.getAccountLockedUntil())) {
            user.setStatus(AccountStatus.ACTIVE);
            user.setFailedLoginAttempts(0);
            user.setAccountLockedUntil(null);
            userRepository.save(user);
            return;
        }

        String until = user.getAccountLockedUntil() != null
                ? " until " + user.getAccountLockedUntil()
                : "";
        throw new AccountLockedException(
                "Account is locked" + until + ". Please try again later.");
    }

    // increment failed attempts — lock if a threshold reached
    private void handleFailedAttempt(User user,
                                     String ipAddress,
                                     String userAgent,
                                     String identifier) {
        user.setFailedLoginAttempts(user.getFailedLoginAttempts() + 1);

        if (user.getFailedLoginAttempts()
                >= AppConstants.Security.MAX_PIN_ATTEMPTS) {
            user.setStatus(AccountStatus.LOCKED);
            user.setAccountLockedUntil(
                    Instant.now().plus(
                            AppConstants.Security.LOCK_DURATION_MINUTES,
                            ChronoUnit.MINUTES));
            auditService.logAction(AuditAction.ACCOUNT_LOCKED,
                    user.getId(), ipAddress, userAgent,
                    "{\"reason\":\"too many failed login attempts\","
                            + "\"locked_until\":\""
                            + user.getAccountLockedUntil() + "\"}");
        }

        userRepository.saveAndFlush(user);

        auditService.logAction(AuditAction.USER_LOGIN_FAILED,
                user.getId(), ipAddress, userAgent,
                "{\"identifier\":\"" + identifier + "\"}");
    }
}