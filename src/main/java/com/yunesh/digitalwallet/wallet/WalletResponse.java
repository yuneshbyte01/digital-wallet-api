package com.yunesh.digitalwallet.wallet;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record WalletResponse(
        UUID id,
        String currency,
        String status,
        BigDecimal balance,
        Instant createdAt
) {}