package com.yunesh.digitalwallet.wallet;

import com.yunesh.digitalwallet.exception.ResourceNotFoundException;
import com.yunesh.digitalwallet.ledger.LedgerEntryType;
import com.yunesh.digitalwallet.ledger.LedgerRepository;
import com.yunesh.digitalwallet.ledger.LedgerService;
import com.yunesh.digitalwallet.user.User;
import com.yunesh.digitalwallet.user.UserRepository;
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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WalletServiceTest {

    @Mock
    private WalletRepository walletRepository;

    @Mock
    private LedgerService ledgerService;

    @Mock
    private LedgerRepository ledgerRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private WalletService walletService;

    private User user;
    private Wallet wallet;
    private Wallet systemWallet;
    private static final UUID SYSTEM_WALLET_ID =
            UUID.fromString("00000000-0000-0000-0000-000000000000");

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(UUID.randomUUID());
        user.setEmail("test@example.com");

        wallet = new Wallet();
        wallet.setId(UUID.randomUUID());
        wallet.setStatus(WalletStatus.ACTIVE);
        wallet.setCurrency("NPR");

        systemWallet = new Wallet();
        systemWallet.setId(SYSTEM_WALLET_ID);
        systemWallet.setStatus(WalletStatus.ACTIVE);
    }

    @Test
    void deposit_activeWallet_succeeds() {
        when(userRepository.findByEmail("test@example.com"))
                .thenReturn(Optional.of(user));
        when(walletRepository.findByUserIdAndCurrency(user.getId(), "NPR"))
                .thenReturn(Optional.of(wallet));
        when(walletRepository.findById(SYSTEM_WALLET_ID))
                .thenReturn(Optional.of(systemWallet));
        when(ledgerService.computeBalance(wallet.getId()))
                .thenReturn(new BigDecimal("1000.00"));

        DepositRequest request = new DepositRequest(
                new BigDecimal("1000.00"), UUID.randomUUID());

        WalletResponse response = walletService.deposit(
                "test@example.com", request);

        assertThat(response.balance())
                .isEqualByComparingTo("1000.00");
        verify(ledgerService, times(1))
                .createEntryPair(any(), any(), any(),
                        eq(LedgerEntryType.DEPOSIT), any());
    }

    @Test
    void deposit_frozenWallet_throwsException() {
        wallet.setStatus(WalletStatus.FROZEN);

        when(userRepository.findByEmail("test@example.com"))
                .thenReturn(Optional.of(user));
        when(walletRepository.findByUserIdAndCurrency(user.getId(), "NPR"))
                .thenReturn(Optional.of(wallet));

        DepositRequest request = new DepositRequest(
                new BigDecimal("1000.00"), UUID.randomUUID());

        assertThatThrownBy(() ->
                walletService.deposit("test@example.com", request))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void getWallet_balanceComputedFromLedger() {
        when(userRepository.findByEmail("test@example.com"))
                .thenReturn(Optional.of(user));
        when(walletRepository.findByUserIdAndCurrency(user.getId(), "NPR"))
                .thenReturn(Optional.of(wallet));
        when(ledgerService.computeBalance(wallet.getId()))
                .thenReturn(new BigDecimal("2500.00"));

        WalletResponse response = walletService.getWallet("test@example.com");

        assertThat(response.balance())
                .isEqualByComparingTo("2500.00");
        verify(ledgerService, times(1))
                .computeBalance(wallet.getId());
    }
}