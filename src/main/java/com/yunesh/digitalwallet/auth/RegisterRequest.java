package com.yunesh.digitalwallet.auth;

import com.yunesh.digitalwallet.user.Gender;
import com.yunesh.digitalwallet.user.TrustedEmail;
import jakarta.validation.constraints.*;

public record RegisterRequest(

        @NotBlank(message = "Full name is required")
        @Size(max = 100, message = "Full name must not exceed 100 characters")
        String fullName,

        @NotBlank(message = "Email is required")
        @TrustedEmail
        String email,

        @NotBlank(message = "Password is required")
        @Size(min = 8, max = 100, message = "Password must be between 8 and 100 characters")
        String password,

        @NotBlank(message = "Phone is required")
        @Pattern(regexp = "^[0-9]{10}$", message = "Phone number must be exactly 10 digits")
        String phone,

        @NotBlank(message = "PIN is required")
        @Pattern(regexp = "^[0-9]{4}$", message = "PIN must be exactly 4 digits")
        String pin,

        @NotNull(message = "Gender is required")
        Gender gender
) {}