package com.yunesh.digitalwallet.ledger;

import com.yunesh.digitalwallet.wallet.Wallet;
import com.yunesh.digitalwallet.wallet.WalletRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LedgerServiceTest {

    @Mock
    private LedgerRepository ledgerRepository;

    @Mock
    private WalletRepository walletRepository;

    @InjectMocks
    private LedgerService ledgerService;

    private UUID walletId;
    private Wallet wallet;

    @BeforeEach
    void setUp() {
        walletId = UUID.randomUUID();
        wallet = new Wallet();
        wallet.setId(walletId);
    }

    @Test
    void computeBalance_returnsCorrectBalance() {
        when(ledgerRepository.computeBalance(walletId))
                .thenReturn(new BigDecimal("1500.00"));

        BigDecimal balance = ledgerService.computeBalance(walletId);

        assertThat(balance).isEqualByComparingTo("1500.00");
    }

    @Test
    void computeBalance_returnsZeroForNewWallet() {
        when(ledgerRepository.computeBalance(walletId))
                .thenReturn(null);

        BigDecimal balance = ledgerService.computeBalance(walletId);

        assertThat(balance).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void createEntryPair_savesOneEntry() {
        UUID debitWalletId = UUID.randomUUID();
        UUID creditWalletId = UUID.randomUUID();
        Wallet debitWallet = new Wallet();
        debitWallet.setId(debitWalletId);
        Wallet creditWallet = new Wallet();
        creditWallet.setId(creditWalletId);

        when(walletRepository.findById(debitWalletId))
                .thenReturn(Optional.of(debitWallet));
        when(walletRepository.findById(creditWalletId))
                .thenReturn(Optional.of(creditWallet));
        when(ledgerRepository.save(any(LedgerEntry.class)))
                .thenAnswer(i -> i.getArgument(0));

        ledgerService.createEntryPair(
                debitWalletId, creditWalletId,
                new BigDecimal("500.00"),
                LedgerEntryType.TRANSFER,
                UUID.randomUUID());

        verify(ledgerRepository, times(1)).save(any(LedgerEntry.class));
    }
}