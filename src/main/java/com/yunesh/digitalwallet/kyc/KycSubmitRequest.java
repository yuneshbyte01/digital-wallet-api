package com.yunesh.digitalwallet.kyc;

import com.yunesh.digitalwallet.user.KycDocumentType;
import com.yunesh.digitalwallet.user.MaritalStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;

import java.time.LocalDate;

public record KycSubmitRequest(

        @Past(message = "Date of birth must be in the past")
        LocalDate dateOfBirth,

        String address,

        @NotBlank(message = "Father name is required")
        String fatherName,

        @NotBlank(message = "Mother name is required")
        String motherName,

        String grandfatherName,

        @NotNull(message = "Marital status is required")
        MaritalStatus maritalStatus,

        String spouseName,

        @Pattern(regexp = "^[0-9]{10}$",
                message = "Spouse phone must be exactly 10 digits")
        String spousePhone,

        @NotNull(message = "Document type is required")
        KycDocumentType documentType,

        @NotBlank(message = "Document ID is required")
        String documentId,

        LocalDate documentIssueDate,

        String documentIssuedPlace
) {}