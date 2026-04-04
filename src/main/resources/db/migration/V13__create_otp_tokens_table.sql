CREATE TABLE otp_tokens (
    id           UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id      UUID        NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    otp          VARCHAR(6)  NOT NULL,
    otp_type     VARCHAR(20) NOT NULL,
    expires_at   TIMESTAMP   NOT NULL,
    used         BOOLEAN     NOT NULL DEFAULT FALSE,
    created_at   TIMESTAMP   NOT NULL DEFAULT now()
);

CREATE INDEX idx_otp_tokens_user_id ON otp_tokens(user_id);
CREATE INDEX idx_otp_tokens_otp     ON otp_tokens(otp);