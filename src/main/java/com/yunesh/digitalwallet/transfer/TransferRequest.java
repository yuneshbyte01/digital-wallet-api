package com.yunesh.digitalwallet.transfer;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.UUID;

public record TransferRequest(

        @NotBlank(message = "Receiver phone is required")
        String receiverPhone,

        @NotNull(message = "Amount is required")
        @DecimalMin(value = "10.00", message = "Minimum transfer amount is NPR 10")
        BigDecimal amount,

        @NotBlank(message = "PIN is required")
        String pin,

        @NotNull(message = "Idempotency key is required")
        UUID idempotencyKey,

        String note
) {}