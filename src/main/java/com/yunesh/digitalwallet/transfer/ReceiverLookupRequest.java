package com.yunesh.digitalwallet.transfer;

import jakarta.validation.constraints.NotBlank;

public record ReceiverLookupRequest(

        @NotBlank(message = "Identifier is required")
        String identifier
) {}