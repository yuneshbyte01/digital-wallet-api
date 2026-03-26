CREATE TABLE statements (
    id           UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id      UUID        NOT NULL REFERENCES users(id),
    month        INTEGER     NOT NULL,
    year         INTEGER     NOT NULL,
    file_path    VARCHAR(500),
    generated_at TIMESTAMP   NOT NULL DEFAULT now(),
    CONSTRAINT uq_statements_user_month_year
        UNIQUE (user_id, month, year)
);

CREATE INDEX idx_statements_user_id ON statements(user_id);