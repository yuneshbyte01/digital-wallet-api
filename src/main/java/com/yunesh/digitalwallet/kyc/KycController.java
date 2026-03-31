package com.yunesh.digitalwallet.kyc;

import com.yunesh.digitalwallet.common.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/users/me/kyc")
@RequiredArgsConstructor
public class KycController {

    private final KycService kycService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<KycDetailResponse> submit(
            @AuthenticationPrincipal String email,
            @Valid @RequestBody KycSubmitRequest request) {
        return ApiResponse.success(
                kycService.submit(email, request),
                "KYC submitted successfully", 201);
    }

    @GetMapping
    public ApiResponse<KycDetailResponse> getMyKyc(
            @AuthenticationPrincipal String email) {
        return ApiResponse.success(kycService.getMyKyc(email));
    }
}