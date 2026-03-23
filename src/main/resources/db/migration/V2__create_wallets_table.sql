CREATE TABLE wallets (
    id          UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id     UUID        NOT NULL REFERENCES users(id),
    currency    VARCHAR(10) NOT NULL DEFAULT 'NPR',
    status      VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    version     INTEGER     NOT NULL DEFAULT 0,
    created_at  TIMESTAMP   NOT NULL DEFAULT now(),
    CONSTRAINT uq_wallet_user_currency UNIQUE (user_id, currency)
);

CREATE INDEX idx_wallets_user_id ON wallets(user_id);