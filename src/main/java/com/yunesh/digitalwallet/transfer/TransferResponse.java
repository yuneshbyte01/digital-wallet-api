package com.yunesh.digitalwallet.transfer;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record TransferResponse(
        UUID id,
        UUID senderWalletId,
        UUID receiverWalletId,
        BigDecimal amount,
        String status,
        UUID idempotencyKey,
        String note,
        Instant createdAt,
        Instant completedAt
) {}