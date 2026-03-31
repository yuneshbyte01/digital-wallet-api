package com.yunesh.digitalwallet.user;

import com.yunesh.digitalwallet.common.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/me")
    public ApiResponse<UserProfileResponse> getMe(
            @AuthenticationPrincipal String email) {
        return ApiResponse.success(userService.findByEmail(email));
    }

    @PutMapping("/me")
    public ApiResponse<UserProfileResponse> updateMe(
            @AuthenticationPrincipal String email,
            @Valid @RequestBody UpdateProfileRequest request) {
        return ApiResponse.success(
                userService.updateProfile(email, request),
                "Profile updated successfully", 200);
    }

    @PostMapping("/me/pin")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void setPin(
            @AuthenticationPrincipal String email,
            @Valid @RequestBody SetPinRequest request) {
        userService.setPin(email, request);
    }
}