package com.yunesh.digitalwallet.kyc;

import com.yunesh.digitalwallet.user.KycStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface KycDetailRepository extends JpaRepository<KycDetail, UUID> {

    Optional<KycDetail> findByUserId(UUID userId);

    boolean existsByUserId(UUID userId);

    @Query("SELECT k FROM KycDetail k WHERE k.user.kycStatus = :status")
    Page<KycDetail> findByUserKycStatus(@Param("status") KycStatus status,
                                        Pageable pageable);
}