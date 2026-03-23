package com.yunesh.digitalwallet.user;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record KycRequest(

        @NotNull(message = "Document type is required")
        KycDocumentType docType,

        @NotBlank(message = "Document number is required")
        String docNumber
) {}