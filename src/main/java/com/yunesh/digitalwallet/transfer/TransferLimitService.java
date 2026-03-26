package com.yunesh.digitalwallet.transfer;

import com.yunesh.digitalwallet.common.AppConstants;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TransferLimitService {

    private final TransferRepository transferRepository;

    @Transactional(readOnly = true)
    public void checkDailyLimit(UUID senderWalletId, BigDecimal amount) {
        Instant startOfDay = Instant.now().truncatedTo(ChronoUnit.DAYS);

        BigDecimal todayTotal = transferRepository
                .sumCompletedSince(senderWalletId, startOfDay);

        if (todayTotal == null) todayTotal = BigDecimal.ZERO;

        if (todayTotal.add(amount)
                .compareTo(AppConstants.Transfer.DAILY_LIMIT_NPR) > 0) {
            throw new com.yunesh.digitalwallet.exception
                    .DailyLimitExceededException(
                    "Daily transfer limit of NPR "
                            + AppConstants.Transfer.DAILY_LIMIT_NPR
                            + " exceeded");
        }
    }
}