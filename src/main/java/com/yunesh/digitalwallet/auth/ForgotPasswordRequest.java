package com.yunesh.digitalwallet.auth;

import com.yunesh.digitalwallet.user.TrustedEmail;
import jakarta.validation.constraints.NotBlank;

public record ForgotPasswordRequest(
        @NotBlank(message = "Email is required")
        @TrustedEmail
        String email
) {}