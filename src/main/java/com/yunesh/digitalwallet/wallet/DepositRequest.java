package com.yunesh.digitalwallet.wallet;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.UUID;

public record DepositRequest(

        @NotNull(message = "Amount is required")
        @DecimalMin(value = "10.00", message = "Minimum deposit amount is NPR 10")
        BigDecimal amount,

        @NotNull(message = "Idempotency key is required")
        UUID idempotencyKey
) {}