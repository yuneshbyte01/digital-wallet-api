package com.yunesh.digitalwallet.user;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UpdateProfileRequest(

        @NotBlank(message = "Full name is required")
        @Size(max = 100, message = "Full name must not exceed 100 characters")
        String fullName,

        @NotBlank(message = "Phone is required")
        @Pattern(regexp = "^\\+?[0-9]{7,15}$", message = "Phone number must be valid")
        String phone
) {}