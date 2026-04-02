package com.yunesh.digitalwallet.user;

import java.time.Instant;
import java.util.UUID;

public record UserProfileResponse(
        UUID id,
        String fullName,
        String email,
        String phone,
        String gender,
        String role,
        String status,
        String kycStatus,
        String profilePictureUrl,
        Instant lastLoginAt,
        Instant createdAt
) {}