package com.yunesh.digitalwallet.audit;

public enum AuditAction {
    USER_LOGIN,
    USER_LOGIN_FAILED,
    TRANSFER_COMPLETED,
    TRANSFER_SELF_REJECTED,
    ACCOUNT_LOCKED,
    TOKEN_REVOKED
}
