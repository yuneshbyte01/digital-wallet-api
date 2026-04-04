package com.yunesh.digitalwallet.auth;

import com.yunesh.digitalwallet.exception.InvalidOtpException;
import com.yunesh.digitalwallet.exception.ResourceNotFoundException;
import com.yunesh.digitalwallet.user.User;
import com.yunesh.digitalwallet.user.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class OtpService {

    private final OtpTokenRepository otpTokenRepository;
    private final UserRepository userRepository;

    private static final int OTP_EXPIRY_MINUTES = 5;
    private static final SecureRandom RANDOM = new SecureRandom();

    @Transactional
    public void generateAndSendForgotPasswordOtp(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No account found with email: " + email));

        // delete any existing unused OTPs for this user and type
        otpTokenRepository.deleteByUserIdAndOtpType(
                user.getId(), OtpType.FORGOT_PASSWORD);

        String otp = generateOtp();

        OtpToken token = OtpToken.builder()
                .user(user)
                .otp(otp)
                .otpType(OtpType.FORGOT_PASSWORD)
                .expiresAt(Instant.now().plus(
                        OTP_EXPIRY_MINUTES, ChronoUnit.MINUTES))
                .used(false)
                .build();

        otpTokenRepository.save(token);

        // TODO: replace with email service when ready
        log.info("============================================");
        log.info("PASSWORD RESET OTP for {}: {}", email, otp);
        log.info("Expires in {} minutes", OTP_EXPIRY_MINUTES);
        log.info("============================================");
    }

    @Transactional
    public void generateAndSendForgotPinOtp(String phone) {
        User user = userRepository.findByPhone(phone)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No account found with phone: " + phone));

        // delete any existing unused OTPs for this user and type
        otpTokenRepository.deleteByUserIdAndOtpType(
                user.getId(), OtpType.FORGOT_PIN);

        String otp = generateOtp();

        OtpToken token = OtpToken.builder()
                .user(user)
                .otp(otp)
                .otpType(OtpType.FORGOT_PIN)
                .expiresAt(Instant.now().plus(
                        OTP_EXPIRY_MINUTES, ChronoUnit.MINUTES))
                .used(false)
                .build();

        otpTokenRepository.save(token);

        // TODO: replace with SMS service when ready
        log.info("============================================");
        log.info("PIN RESET OTP for {}: {}", phone, otp);
        log.info("Expires in {} minutes", OTP_EXPIRY_MINUTES);
        log.info("============================================");
    }

    @Transactional
    public OtpToken verifyOtp(String otp, OtpType type) {
        OtpToken token = otpTokenRepository
                .findByOtpAndOtpTypeAndUsedFalse(otp, type)
                .orElseThrow(() -> new InvalidOtpException(
                        "Invalid or already used OTP"));

        if (Instant.now().isAfter(token.getExpiresAt())) {
            throw new InvalidOtpException(
                    "OTP has expired. Please request a new one.");
        }

        token.setUsed(true);
        return otpTokenRepository.save(token);
    }

    private String generateOtp() {
        int otp = 100000 + RANDOM.nextInt(900000);
        return String.valueOf(otp);
    }
}