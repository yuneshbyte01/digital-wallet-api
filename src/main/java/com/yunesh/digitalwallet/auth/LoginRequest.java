package com.yunesh.digitalwallet.auth;

import jakarta.validation.constraints.NotBlank;

public record LoginRequest(

        @NotBlank(message = "Identifier is required")
        String identifier,

        String password,

        String pin
) {}