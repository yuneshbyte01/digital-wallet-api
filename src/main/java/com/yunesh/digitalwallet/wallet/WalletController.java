package com.yunesh.digitalwallet.wallet;

import com.yunesh.digitalwallet.common.ApiResponse;
import com.yunesh.digitalwallet.common.AppConstants;
import com.yunesh.digitalwallet.ledger.LedgerEntryResponse;
import com.yunesh.digitalwallet.ledger.LedgerMapper;
import com.yunesh.digitalwallet.ledger.LedgerRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/wallets")
@RequiredArgsConstructor
public class WalletController {

    private final WalletService walletService;
    private final LedgerRepository ledgerRepository;
    private final LedgerMapper ledgerMapper;
    private final WalletRepository walletRepository;

    @PostMapping("/deposit")
    public ApiResponse<WalletResponse> deposit(
            @AuthenticationPrincipal String email,
            @Valid @RequestBody DepositRequest request) {
        return ApiResponse.success(
                walletService.deposit(email, request),
                "Deposit successful", 200);
    }

    @GetMapping("/me")
    public ApiResponse<WalletResponse> getWallet(
            @AuthenticationPrincipal String email) {
        return ApiResponse.success(walletService.getWallet(email));
    }

    @GetMapping("/me/ledger-entries")
    public ApiResponse<Page<LedgerEntryResponse>> getLedgerEntries(
            @AuthenticationPrincipal String email,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        if (size > AppConstants.Pagination.MAX_PAGE_SIZE) {
            size = AppConstants.Pagination.MAX_PAGE_SIZE;
        }

        // get wallet id for this user
        UUID walletId = walletService.getWallet(email).id();

        Page<LedgerEntryResponse> entries = ledgerRepository
                .findByWalletId(walletId,
                        PageRequest.of(page, size,
                                Sort.by("createdAt").descending()))
                .map(ledgerMapper::toResponse);

        return ApiResponse.success(entries);
    }
}