package com.yunesh.digitalwallet.scheduler;

import com.yunesh.digitalwallet.transfer.TransferRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class FraudFlagJob {

    private final TransferRepository transferRepository;

    private static final BigDecimal FRAUD_THRESHOLD =
            new BigDecimal("50000.00");

    @Scheduled(cron = "0 0 2 * * ?")
    public void flagSuspiciousAccounts() {
        log.info("FraudFlagJob started");

        Instant since = Instant.now().minus(24, ChronoUnit.HOURS);

        // log wallets exceeding threshold in last 24h
        transferRepository.findAll().stream()
                .collect(java.util.stream.Collectors.groupingBy(
                        t -> t.getSenderWallet().getId(),
                        java.util.stream.Collectors.reducing(
                                BigDecimal.ZERO,
                                t -> t.getAmount(),
                                BigDecimal::add)))
                .entrySet().stream()
                .filter(e -> e.getValue()
                        .compareTo(FRAUD_THRESHOLD) > 0)
                .forEach(e -> log.warn(
                        "FRAUD FLAG: wallet {} transferred {} in 24h",
                        e.getKey(), e.getValue()));

        log.info("FraudFlagJob completed");
    }
}