CREATE TABLE transfers (
    id                  UUID            PRIMARY KEY DEFAULT gen_random_uuid(),
    sender_wallet_id    UUID            NOT NULL REFERENCES wallets(id),
    receiver_wallet_id  UUID            NOT NULL REFERENCES wallets(id),
    amount              NUMERIC(19,4)   NOT NULL CHECK (amount > 0),
    status              VARCHAR(20)     NOT NULL,
    idempotency_key     UUID            NOT NULL UNIQUE,
    note                VARCHAR(255),
    created_at          TIMESTAMP       NOT NULL DEFAULT now(),
    completed_at        TIMESTAMP
);

CREATE INDEX idx_transfers_sender_wallet_id ON transfers(sender_wallet_id);
CREATE INDEX idx_transfers_receiver_wallet_id ON transfers(receiver_wallet_id);
CREATE INDEX idx_transfers_idempotency_key ON transfers(idempotency_key);
CREATE INDEX idx_transfers_created_at ON transfers(created_at);