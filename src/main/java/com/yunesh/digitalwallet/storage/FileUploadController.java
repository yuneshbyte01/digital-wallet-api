package com.yunesh.digitalwallet.storage;

import com.yunesh.digitalwallet.common.ApiResponse;
import com.yunesh.digitalwallet.exception.ResourceNotFoundException;
import com.yunesh.digitalwallet.kyc.KycDetail;
import com.yunesh.digitalwallet.kyc.KycDetailRepository;
import com.yunesh.digitalwallet.user.User;
import com.yunesh.digitalwallet.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/users/me")
@RequiredArgsConstructor
public class FileUploadController {

    private final FileUploadService fileUploadService;
    private final UserRepository userRepository;
    private final KycDetailRepository kycDetailRepository;

    @PostMapping("/profile-photo")
    public ApiResponse<String> uploadProfilePhoto(
            @AuthenticationPrincipal String email,
            @RequestParam("file") MultipartFile file) {

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "User not found"));

        String path = fileUploadService.uploadProfilePhoto(file, user.getId());

        KycDetail detail = kycDetailRepository
                .findByUserId(user.getId())
                .orElse(KycDetail.builder().user(user).build());

        detail.setProfilePicturePath(path);
        kycDetailRepository.save(detail);

        return ApiResponse.success(path, "Profile photo uploaded", 200);
    }

    @PostMapping("/kyc/document-photo")
    public ApiResponse<String> uploadDocumentPhoto(
            @AuthenticationPrincipal String email,
            @RequestParam("file") MultipartFile file) {

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "User not found"));

        String path = fileUploadService.uploadDocumentPhoto(file, user.getId());

        KycDetail detail = kycDetailRepository
                .findByUserId(user.getId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Submit KYC details first before uploading document"));

        detail.setDocumentPicturePath(path);
        kycDetailRepository.save(detail);

        return ApiResponse.success(path, "Document photo uploaded", 200);
    }
}