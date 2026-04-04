package com.yunesh.digitalwallet.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record ForgotPinRequest(
        @NotBlank(message = "Phone is required")
        @Pattern(regexp = "^[0-9]{10}$",
                message = "Phone must be exactly 10 digits")
        String phone
) {}