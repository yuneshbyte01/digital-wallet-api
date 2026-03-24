package com.yunesh.digitalwallet.ledger;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record LedgerEntryResponse(
        UUID id,
        UUID idempotencyKey,
        UUID debitWalletId,
        UUID creditWalletId,
        BigDecimal amount,
        String entryType,
        Instant createdAt
) {}