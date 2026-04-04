package com.yunesh.digitalwallet.auth;

import com.yunesh.digitalwallet.config.EncoderConfig;
import com.yunesh.digitalwallet.exception.ResourceNotFoundException;
import com.yunesh.digitalwallet.user.User;
import com.yunesh.digitalwallet.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PasswordResetService {

    private final OtpService otpService;
    private final UserRepository userRepository;
    private final EncoderConfig encoderConfig;

    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        OtpToken token = otpService.verifyOtp(
                request.otp(), OtpType.FORGOT_PASSWORD);

        User user = token.getUser();

        // verify email matches the OTP owner
        if (!user.getEmail().equalsIgnoreCase(request.email())) {
            throw new BadCredentialsException("OTP does not match email");
        }

        user.setPasswordHash(encoderConfig.getPasswordEncoder()
                .encode(request.newPassword()));
        user.setFailedLoginAttempts(0);
        user.setAccountLockedUntil(null);
        userRepository.save(user);
    }

    @Transactional
    public void resetPin(ResetPinRequest request) {
        OtpToken token = otpService.verifyOtp(
                request.otp(), OtpType.FORGOT_PIN);

        User user = token.getUser();

        // verify phone matches the OTP owner
        if (!user.getPhone().equals(request.phone())) {
            throw new BadCredentialsException("OTP does not match phone");
        }

        user.setPinHash(encoderConfig.getPinEncoder()
                .encode(request.newPin()));
        user.setFailedLoginAttempts(0);
        user.setAccountLockedUntil(null);
        userRepository.save(user);
    }

    @Transactional
    public void changePassword(String email, ChangePasswordRequest request) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "User not found"));

        if (!encoderConfig.getPasswordEncoder().matches(
                request.currentPassword(), user.getPasswordHash())) {
            throw new BadCredentialsException("Current password is incorrect");
        }

        if (encoderConfig.getPasswordEncoder().matches(
                request.newPassword(), user.getPasswordHash())) {
            throw new IllegalArgumentException(
                    "New password must be different from current password");
        }

        user.setPasswordHash(encoderConfig.getPasswordEncoder()
                .encode(request.newPassword()));
        userRepository.save(user);
    }

    @Transactional
    public void changePin(String email, ChangePinRequest request) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "User not found"));

        if (!encoderConfig.getPinEncoder().matches(
                request.currentPin(), user.getPinHash())) {
            throw new BadCredentialsException("Current PIN is incorrect");
        }

        if (encoderConfig.getPinEncoder().matches(
                request.newPin(), user.getPinHash())) {
            throw new IllegalArgumentException(
                    "New PIN must be different from current PIN");
        }

        user.setPinHash(encoderConfig.getPinEncoder()
                .encode(request.newPin()));
        userRepository.save(user);
    }
}