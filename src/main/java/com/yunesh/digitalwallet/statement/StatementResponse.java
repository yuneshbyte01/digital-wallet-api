package com.yunesh.digitalwallet.statement;

import java.time.Instant;
import java.util.UUID;

public record StatementResponse(
        UUID id,
        int month,
        int year,
        String filePath,
        Instant generatedAt
) {}