package com.yunesh.digitalwallet.ledger;

import com.yunesh.digitalwallet.exception.ResourceNotFoundException;
import com.yunesh.digitalwallet.wallet.Wallet;
import com.yunesh.digitalwallet.wallet.WalletRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class LedgerService {

    private final LedgerRepository ledgerRepository;
    private final WalletRepository walletRepository;

    @Transactional(readOnly = true)
    public BigDecimal computeBalance(UUID walletId) {
        BigDecimal balance = ledgerRepository.computeBalance(walletId);
        return balance == null ? BigDecimal.ZERO : balance;
    }

    @Transactional
    public void createEntryPair(UUID debitWalletId,
                                UUID creditWalletId,
                                BigDecimal amount,
                                LedgerEntryType entryType,
                                UUID idempotencyKey) {

        Wallet debitWallet = walletRepository.findById(debitWalletId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Debit wallet not found: " + debitWalletId));

        Wallet creditWallet = walletRepository.findById(creditWalletId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Credit wallet not found: " + creditWalletId));

        LedgerEntry entry = LedgerEntry.builder()
                .idempotencyKey(idempotencyKey)
                .debitWallet(debitWallet)
                .creditWallet(creditWallet)
                .amount(amount)
                .entryType(entryType)
                .build();

        ledgerRepository.save(entry);
    }
}