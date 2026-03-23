package com.yunesh.digitalwallet.user;

import java.time.Instant;
import java.util.UUID;

public record UserProfileResponse(
        UUID id,
        String fullName,
        String email,
        String phone,
        String role,
        String status,
        String kycStatus,
        Instant createdAt
) {}