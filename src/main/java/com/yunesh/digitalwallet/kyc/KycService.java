package com.yunesh.digitalwallet.kyc;

import com.yunesh.digitalwallet.exception.ResourceNotFoundException;
import com.yunesh.digitalwallet.user.KycStatus;
import com.yunesh.digitalwallet.user.MaritalStatus;
import com.yunesh.digitalwallet.user.User;
import com.yunesh.digitalwallet.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class KycService {

    private final KycDetailRepository kycDetailRepository;
    private final UserRepository userRepository;
    private final KycMapper kycMapper;

    @Transactional
    public KycDetailResponse submit(String email, KycSubmitRequest request) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "User not found: " + email));

        // validate spouse fields
        if (request.maritalStatus() == MaritalStatus.MARRIED) {
            if (request.spouseName() == null || request.spouseName().isBlank()) {
                throw new IllegalArgumentException(
                        "Spouse name is required when married");
            }
        }

        KycDetail detail = kycDetailRepository
                .findByUserId(user.getId())
                .orElse(KycDetail.builder().user(user).build());

        detail.setDateOfBirth(request.dateOfBirth());
        detail.setAddress(request.address());
        detail.setFatherName(request.fatherName());
        detail.setMotherName(request.motherName());
        detail.setGrandfatherName(request.grandfatherName());
        detail.setMaritalStatus(request.maritalStatus());
        detail.setDocumentType(request.documentType());
        detail.setDocumentId(request.documentId());
        detail.setDocumentIssueDate(request.documentIssueDate());
        detail.setDocumentIssuedPlace(request.documentIssuedPlace());
        detail.setSubmittedAt(Instant.now());

        // spouse details — only if married
        if (request.maritalStatus() == MaritalStatus.MARRIED) {
            detail.setSpouseName(request.spouseName());
            detail.setSpousePhone(request.spousePhone());
        } else {
            detail.setSpouseName(null);
            detail.setSpousePhone(null);
        }

        KycDetail saved = kycDetailRepository.save(detail);

        // update user kyc status
        user.setKycStatus(KycStatus.PENDING);
        userRepository.save(user);

        return kycMapper.toResponse(saved);
    }

    @Transactional(readOnly = true)
    public KycDetailResponse getMyKyc(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "User not found: " + email));

        KycDetail detail = kycDetailRepository
                .findByUserId(user.getId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "KYC not submitted yet"));

        return kycMapper.toResponse(detail);
    }

    @Transactional
    public KycDetailResponse review(UUID userId,
                                    UUID reviewerId,
                                    KycReviewRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "User not found: " + userId));

        User reviewer = userRepository.findById(reviewerId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Reviewer not found"));

        KycDetail detail = kycDetailRepository
                .findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "KYC not found for user: " + userId));

        // validate rejection reason
        if (request.status() == KycStatus.REJECTED
                && (request.rejectionReason() == null
                || request.rejectionReason().isBlank())) {
            throw new IllegalArgumentException(
                    "Rejection reason is required when rejecting KYC");
        }

        detail.setReviewedAt(Instant.now());
        detail.setReviewedBy(reviewer);
        detail.setRejectionReason(request.rejectionReason());

        kycDetailRepository.save(detail);

        user.setKycStatus(request.status());
        userRepository.save(user);

        return kycMapper.toResponse(detail);
    }

    @Transactional(readOnly = true)
    public Page<KycDetailResponse> getPendingKyc(Pageable pageable) {
        return kycDetailRepository
                .findByUserKycStatus(KycStatus.PENDING, pageable)
                .map(kycMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public KycDetailResponse getKycByUserId(UUID userId) {
        KycDetail detail = kycDetailRepository
                .findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "KYC not found for user: " + userId));
        return kycMapper.toResponse(detail);
    }
}