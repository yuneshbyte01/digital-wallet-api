package com.yunesh.digitalwallet.transfer;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.util.UUID;

public record TransferRequest(

        @NotBlank(message = "Receiver identifier is required")
        String receiverIdentifier,

        @NotNull(message = "Amount is required")
        @DecimalMin(value = "10.00",
                message = "Minimum transfer amount is NPR 10")
        @DecimalMax(value = "25000.00",
                message = "Maximum single transfer is NPR 25,000")
        @Digits(integer = 15, fraction = 4,
                message = "Amount must be a valid monetary value")
        BigDecimal amount,

        @NotNull(message = "Purpose is required")
        TransferPurpose purpose,

        @Size(max = 500,
                message = "Remarks must not exceed 500 characters")
        String remarks,

        @NotBlank(message = "PIN is required")
        @Pattern(regexp = "^[0-9]{4}$",
                message = "PIN must be exactly 4 digits")
        String pin,

        @NotNull(message = "Idempotency key is required")
        UUID idempotencyKey
) {}