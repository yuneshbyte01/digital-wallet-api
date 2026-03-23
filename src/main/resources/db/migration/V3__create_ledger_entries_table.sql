CREATE TABLE ledger_entries (
    id                  UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    idempotency_key     UUID         NOT NULL UNIQUE,
    debit_wallet_id     UUID         NOT NULL,
    credit_wallet_id    UUID         NOT NULL,
    amount              NUMERIC(19,4) NOT NULL CHECK (amount > 0),
    entry_type          VARCHAR(30)  NOT NULL,
    reference_id        UUID,
    description         VARCHAR(255),
    created_by          UUID,
    created_at          TIMESTAMP    NOT NULL DEFAULT now(),

    CONSTRAINT fk_ledger_debit_wallet
        FOREIGN KEY (debit_wallet_id) REFERENCES wallets(id),

    CONSTRAINT fk_ledger_credit_wallet
        FOREIGN KEY (credit_wallet_id) REFERENCES wallets(id),

    CONSTRAINT fk_ledger_created_by
        FOREIGN KEY (created_by) REFERENCES users(id)
);