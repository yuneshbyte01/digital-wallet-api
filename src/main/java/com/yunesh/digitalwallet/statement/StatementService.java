package com.yunesh.digitalwallet.statement;

import com.yunesh.digitalwallet.exception.ResourceNotFoundException;
import com.yunesh.digitalwallet.ledger.LedgerEntry;
import com.yunesh.digitalwallet.ledger.LedgerRepository;
import com.yunesh.digitalwallet.user.User;
import com.yunesh.digitalwallet.user.UserRepository;
import com.yunesh.digitalwallet.wallet.Wallet;
import com.yunesh.digitalwallet.wallet.WalletRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.io.FileOutputStream;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class StatementService {

    private final StatementRepository statementRepository;
    private final UserRepository userRepository;
    private final WalletRepository walletRepository;
    private final LedgerRepository ledgerRepository;

    private static final String STATEMENT_DIR = "statements/";

    @Transactional
    public StatementResponse generate(UUID userId, int month, int year) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "User not found: " + userId));

        // check if already exists
        return statementRepository
                .findByUserIdAndMonthAndYear(userId, month, year)
                .map(s -> new StatementResponse(
                        s.getId(), s.getMonth(), s.getYear(),
                        s.getFilePath(), s.getGeneratedAt()))
                .orElseGet(() -> generateNew(user, month, year));
    }

    private StatementResponse generateNew(User user, int month, int year) {
        Wallet wallet = walletRepository
                .findByUserIdAndCurrency(user.getId(), "NPR")
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Wallet not found for user: " + user.getId()));

        // get ledger entries for the month
        LocalDate start = LocalDate.of(year, month, 1);
        LocalDate end = start.plusMonths(1);

        Instant startInstant = start.atStartOfDay().toInstant(ZoneOffset.UTC);
        Instant endInstant = end.atStartOfDay().toInstant(ZoneOffset.UTC);

        BigDecimal balance = ledgerRepository.computeBalance(wallet.getId());

        // generate simple text-based PDF path
        String fileName = String.format("statement_%s_%d_%d.txt",
                user.getId(), year, month);
        String filePath = STATEMENT_DIR + fileName;

        // ensure directory exists
        new File(STATEMENT_DIR).mkdirs();

        // write simple statement file
        try (FileOutputStream fos = new FileOutputStream(filePath)) {
            String content = buildStatementContent(
                    user, month, year, balance);
            fos.write(content.getBytes());
        } catch (Exception e) {
            log.error("Failed to generate statement file: {}", e.getMessage());
            filePath = null;
        }

        Statement statement = Statement.builder()
                .user(user)
                .month(month)
                .year(year)
                .filePath(filePath)
                .build();

        Statement saved = statementRepository.save(statement);

        return new StatementResponse(
                saved.getId(), saved.getMonth(), saved.getYear(),
                saved.getFilePath(), saved.getGeneratedAt());
    }

    private String buildStatementContent(User user, int month,
                                         int year, BigDecimal balance) {
        return String.format("""
                =====================================
                DIGITAL WALLET - ACCOUNT STATEMENT
                =====================================
                Account Holder : %s
                Email          : %s
                Period         : %d/%d
                Generated At   : %s
                -------------------------------------
                Closing Balance: NPR %s
                =====================================
                """,
                user.getFullName(),
                user.getEmail(),
                month, year,
                Instant.now(),
                balance.toPlainString());
    }

    @Transactional(readOnly = true)
    public List<StatementResponse> getUserStatements(UUID userId) {
        return statementRepository.findByUserId(userId)
                .stream()
                .map(s -> new StatementResponse(
                        s.getId(), s.getMonth(), s.getYear(),
                        s.getFilePath(), s.getGeneratedAt()))
                .toList();
    }

    @Transactional(readOnly = true)
    public StatementResponse getStatement(UUID requesterId,
                                          UUID ownerId,
                                          int month, int year) {
        // ownership check
        if (!requesterId.equals(ownerId)) {
            throw new ResourceNotFoundException(
                    "Statement not found");
        }

        return statementRepository
                .findByUserIdAndMonthAndYear(ownerId, month, year)
                .map(s -> new StatementResponse(
                        s.getId(), s.getMonth(), s.getYear(),
                        s.getFilePath(), s.getGeneratedAt()))
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Statement not found for " + month + "/" + year));
    }
}