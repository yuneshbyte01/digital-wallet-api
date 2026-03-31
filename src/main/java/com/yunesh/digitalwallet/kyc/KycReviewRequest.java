package com.yunesh.digitalwallet.kyc;

import com.yunesh.digitalwallet.user.KycStatus;
import jakarta.validation.constraints.NotNull;

public record KycReviewRequest(

        @NotNull(message = "Status is required")
        KycStatus status,

        String rejectionReason
) {}