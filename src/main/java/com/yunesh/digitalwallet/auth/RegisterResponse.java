package com.yunesh.digitalwallet.auth;

import java.util.UUID;

public record RegisterResponse(
        UUID userId,
        String fullName,
        String email,
        String phone,
        String role,
        String kycStatus
) {}