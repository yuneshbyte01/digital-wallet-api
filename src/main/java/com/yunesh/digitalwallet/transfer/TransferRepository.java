package com.yunesh.digitalwallet.transfer;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public interface TransferRepository extends JpaRepository<Transfer, UUID> {

    boolean existsByIdempotencyKey(UUID idempotencyKey);

    Transfer findByIdempotencyKey(UUID idempotencyKey);

    Page<Transfer> findBySenderWalletId(UUID senderWalletId,
                                        Pageable pageable);

    Page<Transfer> findBySenderWalletIdOrReceiverWalletId(
            UUID senderWalletId,
            UUID receiverWalletId,
            Pageable pageable);

    @Query("""
            SELECT COALESCE(SUM(t.amount), 0)
            FROM Transfer t
            WHERE t.senderWallet.id = :walletId
            AND t.status = 'COMPLETED'
            AND t.createdAt >= :since
            """)
    BigDecimal sumCompletedSince(@Param("walletId") UUID walletId,
                                 @Param("since") Instant since);
}