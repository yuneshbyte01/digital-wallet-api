package com.yunesh.digitalwallet.transfer;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record TransferResponse(
        UUID transferId,
        String transactionCode,
        String status,
        Instant processedAt,
        TransferParticipant sender,
        TransferParticipant receiver,
        BigDecimal amount,
        String currency,
        String purpose,
        String remarks
) {
    public record TransferParticipant(
            String fullName,
            String phone,
            String email
    ) {}
}