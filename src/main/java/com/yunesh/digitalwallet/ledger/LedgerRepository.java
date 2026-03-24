package com.yunesh.digitalwallet.ledger;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.math.BigDecimal;
import java.util.UUID;

public interface LedgerRepository extends JpaRepository<LedgerEntry, UUID> {

    boolean existsByIdempotencyKey(UUID idempotencyKey);

    @Query("""
        SELECT COALESCE(
            SUM(
                CASE
                    WHEN le.creditWallet.id = :walletId THEN le.amount
                    WHEN le.debitWallet.id = :walletId THEN -le.amount
                    ELSE 0
                END
            ),
            0
        )
        FROM LedgerEntry le
        WHERE le.creditWallet.id = :walletId
           OR le.debitWallet.id = :walletId
        """)
    BigDecimal computeBalance(UUID walletId);

    @Query("""
        SELECT le
        FROM LedgerEntry le
        WHERE le.creditWallet.id = :walletId
           OR le.debitWallet.id = :walletId
        """)
    Page<LedgerEntry> findByWalletId(UUID walletId, Pageable pageable);
}