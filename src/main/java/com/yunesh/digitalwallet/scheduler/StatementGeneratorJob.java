package com.yunesh.digitalwallet.scheduler;

import com.yunesh.digitalwallet.statement.StatementService;
import com.yunesh.digitalwallet.user.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Slf4j
@Component
@RequiredArgsConstructor
public class StatementGeneratorJob {

    private final StatementService statementService;
    private final UserRepository userRepository;

    @Scheduled(cron = "0 0 6 1 * ?")
    public void generateMonthlyStatements() {
        LocalDate lastMonth = LocalDate.now().minusMonths(1);
        int month = lastMonth.getMonthValue();
        int year = lastMonth.getYear();

        log.info("Generating statements for {}/{}", month, year);

        userRepository.findAll().forEach(user -> {
            try {
                statementService.generate(user.getId(), month, year);
                log.info("Statement generated for user: {}", user.getEmail());
            } catch (Exception e) {
                log.error("Failed to generate statement for user {}: {}",
                        user.getEmail(), e.getMessage());
            }
        });

        log.info("Statement generation complete for {}/{}", month, year);
    }
}