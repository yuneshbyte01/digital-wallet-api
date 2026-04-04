package com.yunesh.digitalwallet.exception;

import com.yunesh.digitalwallet.common.ErrorResponse;
import com.yunesh.digitalwallet.common.ValidationErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.List;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleIllegalArgument(
            IllegalArgumentException ex,
            HttpServletRequest request) {

        log.warn("Illegal argument at {}: {}", request.getRequestURI(), ex.getMessage());
        return new ErrorResponse(400, "Bad Request", ex.getMessage(),
                request.getRequestURI(), Instant.now());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ValidationErrorResponse handleValidation(
            MethodArgumentNotValidException ex,
            HttpServletRequest request) {

        List<ValidationErrorResponse.FieldError> errors = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(f -> new ValidationErrorResponse.FieldError(
                        f.getField(), f.getDefaultMessage()))
                .toList();

        log.warn("Validation failed at {}: {}", request.getRequestURI(), errors);
        return new ValidationErrorResponse(400, "Validation failed", errors, Instant.now());
    }

    @ExceptionHandler(EmailAlreadyExistsException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ErrorResponse handleEmailExists(
            EmailAlreadyExistsException ex,
            HttpServletRequest request) {

        log.warn("Email conflict at {}: {}", request.getRequestURI(), ex.getMessage());
        return new ErrorResponse(409, "Conflict", ex.getMessage(),
                request.getRequestURI(), Instant.now());
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ErrorResponse handleNotFound(
            ResourceNotFoundException ex,
            HttpServletRequest request) {

        log.warn("Not found at {}: {}", request.getRequestURI(), ex.getMessage());
        return new ErrorResponse(404, "Not Found", ex.getMessage(),
                request.getRequestURI(), Instant.now());
    }

    @ExceptionHandler(BadCredentialsException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public ErrorResponse handleBadCredentials(
            BadCredentialsException ex,
            HttpServletRequest request) {

        log.warn("Bad credentials at {}", request.getRequestURI());
        return new ErrorResponse(401, "Unauthorized", ex.getMessage(),
                request.getRequestURI(), Instant.now());
    }

    @ExceptionHandler(InvalidTokenException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public ErrorResponse handleInvalidToken(
            InvalidTokenException ex,
            HttpServletRequest request) {

        log.warn("Invalid token at {}: {}", request.getRequestURI(), ex.getMessage());
        return new ErrorResponse(401, "Unauthorized", ex.getMessage(),
                request.getRequestURI(), Instant.now());
    }

    @ExceptionHandler(DuplicateLedgerEntryException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ErrorResponse handleDuplicateLedgerEntry(
            DuplicateLedgerEntryException ex,
            HttpServletRequest request) {

        log.warn("Duplicate ledger entry at {}: {}", request.getRequestURI(), ex.getMessage());
        return new ErrorResponse(409, "Conflict", ex.getMessage(),
                request.getRequestURI(), Instant.now());
    }

    @ExceptionHandler(WalletFrozenException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleWalletFrozen(
            WalletFrozenException ex,
            HttpServletRequest request) {

        log.warn("Wallet frozen at {}: {}", request.getRequestURI(), ex.getMessage());
        return new ErrorResponse(400, "Bad Request", ex.getMessage(),
                request.getRequestURI(), Instant.now());
    }

    @ExceptionHandler(InsufficientFundsException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleInsufficientFunds(
            InsufficientFundsException ex,
            HttpServletRequest request) {

        log.warn("Insufficient funds at {}: {}", request.getRequestURI(), ex.getMessage());
        return new ErrorResponse(400, "Bad Request", ex.getMessage(),
                request.getRequestURI(), Instant.now());
    }

    @ExceptionHandler(DuplicateTransferException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ErrorResponse handleDuplicateTransfer(
            DuplicateTransferException ex,
            HttpServletRequest request) {

        log.warn("Duplicate transfer at {}: {}", request.getRequestURI(), ex.getMessage());
        return new ErrorResponse(409, "Conflict", ex.getMessage(),
                request.getRequestURI(), Instant.now());
    }

    @ExceptionHandler(SelfTransferException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleSelfTransfer(
            SelfTransferException ex,
            HttpServletRequest request) {

        log.warn("Self transfer attempt at {}", request.getRequestURI());
        return new ErrorResponse(400, "Bad Request", ex.getMessage(),
                request.getRequestURI(), Instant.now());
    }

    @ExceptionHandler(AccountLockedException.class)
    @ResponseStatus(HttpStatus.LOCKED)
    public ErrorResponse handleAccountLocked(
            AccountLockedException ex,
            HttpServletRequest request) {

        log.warn("Account locked at {}: {}", request.getRequestURI(), ex.getMessage());
        return new ErrorResponse(423, "Locked", ex.getMessage(),
                request.getRequestURI(), Instant.now());
    }

    @ExceptionHandler(DailyLimitExceededException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleDailyLimit(
            DailyLimitExceededException ex,
            HttpServletRequest request) {

        log.warn("Daily limit exceeded at {}: {}", request.getRequestURI(), ex.getMessage());
        return new ErrorResponse(400, "Bad Request", ex.getMessage(),
                request.getRequestURI(), Instant.now());
    }

    @ExceptionHandler(OptimisticLockingFailureException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ErrorResponse handleOptimisticLock(
            OptimisticLockingFailureException ex,
            HttpServletRequest request) {

        log.warn("Optimistic lock failure at {}", request.getRequestURI());
        return new ErrorResponse(409, "Conflict",
                "Transaction conflict — please retry",
                request.getRequestURI(), Instant.now());
    }

    @ExceptionHandler(RateLimitExceededException.class)
    @ResponseStatus(HttpStatus.TOO_MANY_REQUESTS)
    public ErrorResponse handleRateLimit(
            RateLimitExceededException ex,
            HttpServletRequest request,
            HttpServletResponse response) {

        response.setHeader("Retry-After", "60");
        log.warn("Rate limit exceeded at {}", request.getRequestURI());
        return new ErrorResponse(429, "Too Many Requests", ex.getMessage(),
                request.getRequestURI(), Instant.now());
    }

    @ExceptionHandler(PhoneAlreadyExistsException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ErrorResponse handlePhoneExists(
            PhoneAlreadyExistsException ex,
            HttpServletRequest request) {

        log.warn("Phone conflict at {}: {}", request.getRequestURI(), ex.getMessage());
        return new ErrorResponse(409, "Conflict", ex.getMessage(),
                request.getRequestURI(), Instant.now());
    }

    @ExceptionHandler(InvalidOtpException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleInvalidOtp(
            InvalidOtpException ex,
            HttpServletRequest request) {

        log.warn("Invalid OTP at {}: {}", request.getRequestURI(), ex.getMessage());
        return new ErrorResponse(400, "Bad Request", ex.getMessage(),
                request.getRequestURI(), Instant.now());
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ErrorResponse handleGeneric(
            Exception ex,
            HttpServletRequest request) {

        log.error("Unhandled exception at {}", request.getRequestURI(), ex);
        return new ErrorResponse(500, "Internal Server Error",
                "An unexpected error occurred",
                request.getRequestURI(), Instant.now());
    }
}