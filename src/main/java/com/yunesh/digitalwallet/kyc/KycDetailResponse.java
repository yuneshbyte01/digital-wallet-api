package com.yunesh.digitalwallet.kyc;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record KycDetailResponse(
        UUID id,
        UUID userId,
        LocalDate dateOfBirth,
        String address,
        String fatherName,
        String motherName,
        String grandfatherName,
        String maritalStatus,
        String spouseName,
        String spousePhone,
        String documentType,
        String documentId,
        LocalDate documentIssueDate,
        String documentIssuedPlace,
        String profilePicturePath,
        String documentPicturePath,
        String kycStatus,
        Instant submittedAt,
        Instant reviewedAt,
        String rejectionReason,
        Instant createdAt
) {}