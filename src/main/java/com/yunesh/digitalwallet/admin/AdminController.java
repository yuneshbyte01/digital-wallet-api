package com.yunesh.digitalwallet.admin;

import com.yunesh.digitalwallet.common.ApiResponse;
import com.yunesh.digitalwallet.transfer.TransferResponse;
import com.yunesh.digitalwallet.transfer.TransferService;
import com.yunesh.digitalwallet.user.UserProfileResponse;
import com.yunesh.digitalwallet.user.UserService;
import com.yunesh.digitalwallet.wallet.WalletResponse;
import com.yunesh.digitalwallet.wallet.WalletService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
public class AdminController {

    private final WalletService walletService;
    private final TransferService transferService;
    private final UserService userService;

    @PutMapping("/wallets/{walletId}/freeze")
    public ApiResponse<WalletResponse> toggleWalletFreeze(
            @PathVariable UUID walletId) {
        return ApiResponse.success(
                walletService.toggleFreeze(walletId),
                "Wallet status updated", 200);
    }

    @PutMapping("/transfers/{transferId}/reversal")
    public ApiResponse<TransferResponse> reverseTransfer(
            @PathVariable UUID transferId) {
        return ApiResponse.success(
                transferService.reverseTransfer(transferId),
                "Transfer reversed", 200);
    }

    @PutMapping("/users/{userId}/lock")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void toggleUserLock(@PathVariable UUID userId) {
        userService.toggleLock(userId);
    }

    @PutMapping("/users/{userId}/kyc-status")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void updateKycStatus(
            @PathVariable UUID userId,
            @RequestParam String status) {
        userService.updateKycStatus(userId, status);
    }

    @GetMapping("/users")
    public ApiResponse<Page<UserProfileResponse>> getAllUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.success(userService.getAllUsers(page, size));
    }
}