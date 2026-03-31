package com.yunesh.digitalwallet.admin;

import com.yunesh.digitalwallet.common.ApiResponse;
import com.yunesh.digitalwallet.common.AppConstants;
import com.yunesh.digitalwallet.kyc.KycDetailResponse;
import com.yunesh.digitalwallet.kyc.KycReviewRequest;
import com.yunesh.digitalwallet.kyc.KycService;
import com.yunesh.digitalwallet.user.UserRepository;
import com.yunesh.digitalwallet.exception.ResourceNotFoundException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/kyc")
@RequiredArgsConstructor
public class AdminKycController {

    private final KycService kycService;
    private final UserRepository userRepository;

    @GetMapping("/pending")
    public ApiResponse<Page<KycDetailResponse>> getPendingKyc(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        if (size > AppConstants.Pagination.MAX_PAGE_SIZE) {
            size = AppConstants.Pagination.MAX_PAGE_SIZE;
        }

        return ApiResponse.success(kycService.getPendingKyc(
                PageRequest.of(page, size,
                        Sort.by("submittedAt").ascending())));
    }

    @GetMapping("/{userId}")
    public ApiResponse<KycDetailResponse> getKycByUser(
            @PathVariable UUID userId) {
        return ApiResponse.success(kycService.getKycByUserId(userId));
    }

    @PutMapping("/{userId}/review")
    public ApiResponse<KycDetailResponse> review(
            @PathVariable UUID userId,
            @AuthenticationPrincipal String reviewerEmail,
            @Valid @RequestBody KycReviewRequest request) {

        UUID reviewerId = userRepository.findByEmail(reviewerEmail)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Reviewer not found"))
                .getId();

        return ApiResponse.success(
                kycService.review(userId, reviewerId, request),
                "KYC review completed", 200);
    }
}