package com.yunesh.digitalwallet.auth;

import com.yunesh.digitalwallet.common.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final TokenRefreshService tokenRefreshService;
    private final OtpService otpService;
    private final PasswordResetService passwordResetService;

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<RegisterResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ApiResponse.success(authService.register(request), "User registered successfully", 201);
    }

    @PostMapping("/login")
    public ApiResponse<LoginResponse> login(@Valid @RequestBody LoginRequest request,
                                            HttpServletRequest httpRequest) {
        String ipAddress = httpRequest.getRemoteAddr();
        String userAgent = httpRequest.getHeader("User-Agent");
        return ApiResponse.success(
                authService.login(request, ipAddress, userAgent),
                "Login successful", 200);
    }

    @PostMapping("/refresh")
    public ApiResponse<LoginResponse> refresh(
            @Valid @RequestBody TokenRefreshRequest request) {
        return ApiResponse.success(
                tokenRefreshService.refresh(request.refreshToken()),
                "Token refreshed successfully", 200);
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(@Valid @RequestBody LogoutRequest request) {
        authService.logout(request);
    }

    @PostMapping("/forgot-password")
    public ApiResponse<Void> forgotPassword(
            @Valid @RequestBody ForgotPasswordRequest request) {
        otpService.generateAndSendForgotPasswordOtp(request.email());
        return ApiResponse.success(null,
                "OTP sent to your email. Valid for 5 minutes.", 200);
    }

    @PostMapping("/reset-password")
    public ApiResponse<Void> resetPassword(
            @Valid @RequestBody ResetPasswordRequest request) {
        passwordResetService.resetPassword(request);
        return ApiResponse.success(null, "Password reset successfully", 200);
    }

    @PostMapping("/forgot-pin")
    public ApiResponse<Void> forgotPin(
            @Valid @RequestBody ForgotPinRequest request) {
        otpService.generateAndSendForgotPinOtp(request.phone());
        return ApiResponse.success(null,
                "OTP sent to your registered email. Valid for 5 minutes.", 200);
    }

    @PostMapping("/reset-pin")
    public ApiResponse<Void> resetPin(
            @Valid @RequestBody ResetPinRequest request) {
        passwordResetService.resetPin(request);
        return ApiResponse.success(null, "PIN reset successfully", 200);
    }
}