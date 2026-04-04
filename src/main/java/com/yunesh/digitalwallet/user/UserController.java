package com.yunesh.digitalwallet.user;

import com.yunesh.digitalwallet.auth.ChangePasswordRequest;
import com.yunesh.digitalwallet.auth.ChangePinRequest;
import com.yunesh.digitalwallet.auth.PasswordResetService;
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
    private final PasswordResetService passwordResetService;

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

    @PutMapping("/me/password")
    public ApiResponse<Void> changePassword(
            @AuthenticationPrincipal String email,
            @Valid @RequestBody ChangePasswordRequest request) {
        passwordResetService.changePassword(email, request);
        return ApiResponse.success(null, "Password changed successfully", 200);
    }

    @PutMapping("/me/pin")
    public ApiResponse<Void> changePin(
            @AuthenticationPrincipal String email,
            @Valid @RequestBody ChangePinRequest request) {
        passwordResetService.changePin(email, request);
        return ApiResponse.success(null, "PIN changed successfully", 200);
    }
}