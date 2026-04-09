package com.yunesh.digitalwallet.transfer;

import java.util.UUID;

public record ReceiverLookupResponse(
        UUID userId,
        String fullName,
        String identifier,
        UUID walletId
) {}