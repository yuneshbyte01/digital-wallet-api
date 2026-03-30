package com.yunesh.digitalwallet.wallet;

import com.yunesh.digitalwallet.exception.ResourceNotFoundException;
import com.yunesh.digitalwallet.ledger.LedgerEntryType;
import com.yunesh.digitalwallet.ledger.LedgerService;
import com.yunesh.digitalwallet.user.User;
import com.yunesh.digitalwallet.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class WalletService {

    private final WalletRepository walletRepository;
    private final LedgerService ledgerService;
    private final UserRepository userRepository;

    // System wallet ID — represents the external source for deposits
    private static final UUID SYSTEM_WALLET_ID =
            UUID.fromString("00000000-0000-0000-0000-000000000000");

    @Transactional
    public WalletResponse deposit(String email, DepositRequest request) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "User not found: " + email));

        Wallet wallet = walletRepository
                .findByUserIdAndCurrency(user.getId(), "NPR")
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Wallet not found for user: " + email));

        if (wallet.getStatus() != WalletStatus.ACTIVE) {
            throw new IllegalStateException("Wallet is not active");
        }

        // system wallet as debit source
        Wallet systemWallet = walletRepository.findById(SYSTEM_WALLET_ID)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "System wallet not found"));

        ledgerService.createEntryPair(
                systemWallet.getId(),
                wallet.getId(),
                request.amount(),
                LedgerEntryType.DEPOSIT,
                request.idempotencyKey()
        );

        return getWalletResponse(wallet);
    }

    @Transactional(readOnly = true)
    public WalletResponse getWallet(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "User not found: " + email));

        Wallet wallet = walletRepository
                .findByUserIdAndCurrency(user.getId(), "NPR")
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Wallet not found for user: " + email));

        return getWalletResponse(wallet);
    }

    @Transactional
    public WalletResponse toggleFreeze(UUID walletId) {
        Wallet wallet = walletRepository.findById(walletId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Wallet not found: " + walletId));

        if (wallet.getStatus() == WalletStatus.ACTIVE) {
            wallet.setStatus(WalletStatus.FROZEN);
        } else {
            wallet.setStatus(WalletStatus.ACTIVE);
        }

        walletRepository.save(wallet);
        return getWalletResponse(wallet);
    }

    @Transactional(readOnly = true)
    public Page<WalletResponse> getAllWallets(int page, int size) {
        return walletRepository.findAll(
                        PageRequest.of(page, size))
                .map(this::getWalletResponse);
    }

    private WalletResponse getWalletResponse(Wallet wallet) {
        return new WalletResponse(
                wallet.getId(),
                wallet.getCurrency(),
                wallet.getStatus().name(),
                ledgerService.computeBalance(wallet.getId()),
                wallet.getCreatedAt()
        );
    }
}