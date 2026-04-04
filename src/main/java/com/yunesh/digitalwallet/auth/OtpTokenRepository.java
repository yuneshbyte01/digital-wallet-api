package com.yunesh.digitalwallet.auth;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface OtpTokenRepository extends JpaRepository<OtpToken, UUID> {

    Optional<OtpToken> findByOtpAndOtpTypeAndUsedFalse(
            String otp, OtpType otpType);

    @Modifying
    @Query("DELETE FROM OtpToken o WHERE o.user.id = :userId AND o.otpType = :type")
    void deleteByUserIdAndOtpType(@Param("userId") UUID userId,
                                  @Param("type") OtpType otpType);
}