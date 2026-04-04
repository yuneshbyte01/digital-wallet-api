package com.yunesh.digitalwallet.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record ResetPinRequest(
        @NotBlank(message = "OTP is required")
        String otp,

        @NotBlank(message = "Phone is required")
        String phone,

        @NotBlank(message = "New PIN is required")
        @Pattern(regexp = "^[0-9]{4}$",
                message = "PIN must be exactly 4 digits")
        String newPin
) {}