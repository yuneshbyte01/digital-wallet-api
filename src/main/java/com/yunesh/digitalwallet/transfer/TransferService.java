package com.yunesh.digitalwallet.transfer;

import com.yunesh.digitalwallet.audit.AuditAction;
import com.yunesh.digitalwallet.audit.AuditService;
import com.yunesh.digitalwallet.common.AppConstants;
import com.yunesh.digitalwallet.exception.*;
import com.yunesh.digitalwallet.ledger.LedgerEntryType;
import com.yunesh.digitalwallet.ledger.LedgerService;
import com.yunesh.digitalwallet.user.AccountStatus;
import com.yunesh.digitalwallet.user.User;
import com.yunesh.digitalwallet.user.UserRepository;
import com.yunesh.digitalwallet.wallet.Wallet;
import com.yunesh.digitalwallet.wallet.WalletRepository;
import com.yunesh.digitalwallet.wallet.WalletStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TransferService {

    private final TransferRepository transferRepository;
    private final UserRepository userRepository;
    private final WalletRepository walletRepository;
    private final LedgerService ledgerService;
    private final TransferLimitService transferLimitService;
    private final TransferMapper transferMapper;
    private final BCryptPasswordEncoder passwordEncoder;
    private final AuditService auditService;

    @Transactional
    public TransferResponse executeTransfer(String senderEmail,
                                            TransferRequest request) {

        // Step 1 — idempotency check
        if (transferRepository.existsByIdempotencyKey(request.idempotencyKey())) {
            Transfer existing = transferRepository
                    .findByIdempotencyKey(request.idempotencyKey());
            return transferMapper.toResponse(existing);
        }

        // Step 2 — resolve sender
        User sender = userRepository.findByEmail(senderEmail)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Sender not found"));

        Wallet senderWallet = walletRepository
                .findByUserIdAndCurrency(sender.getId(), "NPR")
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Sender wallet not found"));

        // resolve receiver
        User receiver = userRepository.findByPhone(request.receiverPhone())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Receiver not found for phone: "
                                + request.receiverPhone()));

        Wallet receiverWallet = walletRepository
                .findByUserIdAndCurrency(receiver.getId(), "NPR")
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Receiver wallet not found"));

        // Step 2 — self-transfer guard
        if (senderWallet.getId().equals(receiverWallet.getId())) {
            auditService.logAction(AuditAction.TRANSFER_SELF_REJECTED, sender.getId(), null, null,
                    Map.of("idempotencyKey", request.idempotencyKey().toString()));
            throw new SelfTransferException(
                    "Cannot transfer to your own wallet");
        }

        // Step 3 — account status check
        if (sender.getStatus() != AccountStatus.ACTIVE) {
            throw new AccountLockedException(
                    "Sender account is not active");
        }

        if (senderWallet.getStatus() != WalletStatus.ACTIVE) {
            throw new AccountLockedException(
                    "Sender wallet is frozen");
        }

        // Step 4 — PIN verification
        if (sender.getPinHash() == null) {
            throw new AccountLockedException(
                    "PIN not set. Please set your PIN first.");
        }

        if (!passwordEncoder.matches(request.pin(), sender.getPinHash())) {
            sender.setPinAttempts(sender.getPinAttempts() + 1);

            if (sender.getPinAttempts()
                    >= AppConstants.Security.MAX_PIN_ATTEMPTS) {
                sender.setStatus(AccountStatus.LOCKED);
            }

            userRepository.save(sender);

            if (sender.getStatus() == AccountStatus.LOCKED) {
                auditService.logAction(AuditAction.ACCOUNT_LOCKED, sender.getId(), null, null,
                        Map.of("reason", "too many failed PIN attempts"));
            }

            throw new AccountLockedException(
                    sender.getStatus() == AccountStatus.LOCKED
                            ? "Account locked due to too many failed PIN attempts"
                            : "Invalid PIN. Attempts remaining: "
                            + (AppConstants.Security.MAX_PIN_ATTEMPTS
                            - sender.getPinAttempts()));
        }

        // reset pin attempts on success
        if (sender.getPinAttempts() > 0) {
            sender.setPinAttempts(0);
            userRepository.save(sender);
        }

        // Step 5 — balance check
        BigDecimal balance = ledgerService.computeBalance(senderWallet.getId());
        if (balance.compareTo(request.amount()) < 0) {
            throw new InsufficientFundsException(
                    "Insufficient funds. Available: NPR " + balance);
        }

        // Step 6 — daily limit check
        transferLimitService.checkDailyLimit(
                senderWallet.getId(), request.amount());

        // Step 7 — persist transfer PENDING
        Transfer transfer = Transfer.builder()
                .senderWallet(senderWallet)
                .receiverWallet(receiverWallet)
                .amount(request.amount())
                .status(TransferStatus.PENDING)
                .idempotencyKey(request.idempotencyKey())
                .note(request.note())
                .build();

        transferRepository.save(transfer);

        // Step 8 — create ledger entries
        ledgerService.createEntryPair(
                senderWallet.getId(),
                receiverWallet.getId(),
                request.amount(),
                LedgerEntryType.TRANSFER,
                request.idempotencyKey()
        );

        // Step 9 — mark COMPLETED
        transfer.setStatus(TransferStatus.COMPLETED);
        transfer.setCompletedAt(Instant.now());
        transferRepository.save(transfer);

        auditService.logAction(AuditAction.TRANSFER_COMPLETED, sender.getId(), null, null,
                Map.of("transferId", transfer.getId().toString(),
                        "amount", request.amount().toPlainString(),
                        "receiverPhone", request.receiverPhone()));

        return transferMapper.toResponse(transfer);
    }

    @Transactional(readOnly = true)
    public UUID getSenderWalletId(String email) {
        User sender = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "User not found: " + email));

        return walletRepository
                .findByUserIdAndCurrency(sender.getId(), "NPR")
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Wallet not found"))
                .getId();
    }

    @Transactional
    public TransferResponse reverseTransfer(UUID transferId) {
        Transfer transfer = transferRepository.findById(transferId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Transfer not found: " + transferId));

        if (transfer.getStatus() != TransferStatus.COMPLETED) {
            throw new IllegalStateException(
                    "Only COMPLETED transfers can be reversed");
        }

        // reversal — swap debit and credit
        ledgerService.createEntryPair(
                transfer.getReceiverWallet().getId(),
                transfer.getSenderWallet().getId(),
                transfer.getAmount(),
                LedgerEntryType.REVERSAL,
                UUID.randomUUID()
        );

        transfer.setStatus(TransferStatus.REVERSED);
        transferRepository.save(transfer);

        return transferMapper.toResponse(transfer);
    }
}