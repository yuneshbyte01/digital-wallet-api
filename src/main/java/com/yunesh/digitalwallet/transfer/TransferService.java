package com.yunesh.digitalwallet.transfer;

import com.yunesh.digitalwallet.audit.AuditAction;
import com.yunesh.digitalwallet.audit.AuditService;
import com.yunesh.digitalwallet.common.AppConstants;
import com.yunesh.digitalwallet.config.EncoderConfig;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
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
    private final EncoderConfig encoderConfig;
    private final AuditService auditService;
    private final TransactionCodeGenerator transactionCodeGenerator;

    @Transactional(readOnly = true)
    public ReceiverLookupResponse lookupReceiver(String identifier) {
        boolean isEmail = identifier.contains("@");

        User receiver = isEmail
                ? userRepository.findByEmail(identifier)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No user found with email: " + identifier))
                : userRepository.findByPhone(identifier)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No user found with phone: " + identifier));

        Wallet wallet = walletRepository
                .findByUserIdAndCurrency(receiver.getId(), "NPR")
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Receiver wallet not found"));

        return new ReceiverLookupResponse(
                receiver.getId(),
                receiver.getFullName(),
                identifier,
                wallet.getId()
        );
    }

    @Transactional
    public TransferResponse executeTransfer(String senderEmail,
                                            TransferRequest request) {

        // Step 1 — idempotency check
        if (transferRepository.existsByIdempotencyKey(
                request.idempotencyKey())) {
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

        // resolve receiver by phone or email
        String receiverIdentifier = request.receiverIdentifier();
        boolean isEmail = receiverIdentifier.contains("@");

        User receiver = isEmail
                ? userRepository.findByEmail(receiverIdentifier)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No user found with email: "
                                + receiverIdentifier))
                : userRepository.findByPhone(receiverIdentifier)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No user found with phone: "
                                + receiverIdentifier));

        Wallet receiverWallet = walletRepository
                .findByUserIdAndCurrency(receiver.getId(), "NPR")
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Receiver wallet not found"));

        // Step 2 — self-transfer guard
        if (senderWallet.getId().equals(receiverWallet.getId())) {
            auditService.logAction(
                    AuditAction.TRANSFER_SELF_REJECTED,
                    sender.getId(), null, null,
                    "{\"idempotencyKey\":\""
                            + request.idempotencyKey() + "\"}");
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

        if (!encoderConfig.getPinEncoder().matches(
                request.pin(), sender.getPinHash())) {

            sender.setFailedLoginAttempts(
                    sender.getFailedLoginAttempts() + 1);

            if (sender.getFailedLoginAttempts()
                    >= AppConstants.Security.MAX_PIN_ATTEMPTS) {
                sender.setStatus(AccountStatus.LOCKED);
                sender.setAccountLockedUntil(
                        Instant.now().plus(
                                AppConstants.Security.LOCK_DURATION_MINUTES,
                                java.time.temporal.ChronoUnit.MINUTES));
                auditService.logAction(
                        AuditAction.ACCOUNT_LOCKED,
                        sender.getId(), null, null,
                        "{\"reason\":\"too many failed PIN attempts\"}");
            }

            userRepository.save(sender);

            throw new AccountLockedException(
                    sender.getStatus() == AccountStatus.LOCKED
                            ? "Account locked for "
                            + AppConstants.Security.LOCK_DURATION_MINUTES
                            + " minutes due to too many failed PIN attempts"
                            : "Invalid PIN. Attempts remaining: "
                            + (AppConstants.Security.MAX_PIN_ATTEMPTS
                            - sender.getFailedLoginAttempts()));
        }

        // reset attempts on successful PIN
        if (sender.getFailedLoginAttempts() > 0) {
            sender.setFailedLoginAttempts(0);
            sender.setAccountLockedUntil(null);
            userRepository.save(sender);
        }

        // Step 5 — balance check
        BigDecimal balance = ledgerService.computeBalance(
                senderWallet.getId());
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
                .transactionCode(transactionCodeGenerator.generate())
                .purpose(request.purpose())
                .remarks(request.remarks())
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

        // Step 10 — audit log
        auditService.logAction(
                AuditAction.TRANSFER_COMPLETED,
                sender.getId(), null, null,
                "{\"transferId\":\"" + transfer.getId()
                        + "\",\"amount\":\"" + request.amount()
                        + "\",\"receiver\":\""
                        + receiverIdentifier + "\"}");

        return transferMapper.toResponse(transfer);
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

        auditService.logAction(
                AuditAction.TRANSFER_FAILED,
                transfer.getSenderWallet().getUser().getId(),
                null, null,
                "{\"transferId\":\"" + transferId
                        + "\",\"reason\":\"admin reversal\"}");

        return transferMapper.toResponse(transfer);
    }

    @Transactional(readOnly = true)
    public Page<TransferResponse> getMyTransfers(String email,
                                                 int page, int size) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "User not found"));

        Wallet wallet = walletRepository
                .findByUserIdAndCurrency(user.getId(), "NPR")
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Wallet not found"));

        return transferRepository
                .findBySenderWalletIdOrReceiverWalletId(
                        wallet.getId(), wallet.getId(),
                        PageRequest.of(page, size,
                                Sort.by("createdAt").descending()))
                .map(transferMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public Page<TransferResponse> getAllTransfers(int page, int size) {
        return transferRepository.findAll(
                        PageRequest.of(page, size,
                                Sort.by("createdAt").descending()))
                .map(transferMapper::toResponse);
    }
}