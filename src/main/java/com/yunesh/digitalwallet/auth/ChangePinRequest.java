package com.yunesh.digitalwallet.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record ChangePinRequest(
        @NotBlank(message = "Current PIN is required")
        @Pattern(regexp = "^[0-9]{4}$",
                message = "Current PIN must be exactly 4 digits")
        String currentPin,

        @NotBlank(message = "New PIN is required")
        @Pattern(regexp = "^[0-9]{4}$",
                message = "New PIN must be exactly 4 digits")
        String newPin
) {}