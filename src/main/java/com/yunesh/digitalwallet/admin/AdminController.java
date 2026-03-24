package com.yunesh.digitalwallet.admin;

import com.yunesh.digitalwallet.common.ApiResponse;
import com.yunesh.digitalwallet.wallet.WalletResponse;
import com.yunesh.digitalwallet.wallet.WalletService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
public class AdminController {

    private final WalletService walletService;

    @PutMapping("/wallets/{walletId}/freeze")
    public ApiResponse<WalletResponse> toggleWalletFreeze(
            @PathVariable UUID walletId) {
        return ApiResponse.success(
                walletService.toggleFreeze(walletId),
                "Wallet status updated", 200);
    }
}