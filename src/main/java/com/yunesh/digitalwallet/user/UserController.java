package com.yunesh.digitalwallet.user;

import com.yunesh.digitalwallet.common.ApiResponse;
import com.yunesh.digitalwallet.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserRepository userRepository;

    @GetMapping("/me")
    public ApiResponse<UserProfileResponse> getMe(
            @AuthenticationPrincipal String email) {

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "User not found: " + email));

        return ApiResponse.success(new UserProfileResponse(
                user.getId(),
                user.getFullName(),
                user.getEmail(),
                user.getPhone(),
                user.getRole().name(),
                user.getStatus().name(),
                user.getKycStatus().name(),
                user.getCreatedAt()
        ));
    }
}