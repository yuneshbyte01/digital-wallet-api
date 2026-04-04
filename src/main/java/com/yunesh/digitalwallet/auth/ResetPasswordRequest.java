package com.yunesh.digitalwallet.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ResetPasswordRequest(
        @NotBlank(message = "OTP is required")
        String otp,

        @NotBlank(message = "Email is required")
        String email,

        @NotBlank(message = "New password is required")
        @Size(min = 8, max = 100,
                message = "Password must be between 8 and 100 characters")
        String newPassword
) {}