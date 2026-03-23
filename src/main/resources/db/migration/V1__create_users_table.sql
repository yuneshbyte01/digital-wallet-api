CREATE TABLE users (
    id               UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    full_name        VARCHAR(100) NOT NULL,
    email            VARCHAR(255) NOT NULL UNIQUE,
    phone            VARCHAR(20)  NOT NULL UNIQUE,
    password_hash    VARCHAR(255) NOT NULL,
    pin_hash         VARCHAR(255),
    pin_attempts     INTEGER      NOT NULL DEFAULT 0,
    kyc_doc_type     VARCHAR(50),
    kyc_doc_number   VARCHAR(100),
    kyc_status       VARCHAR(20)  NOT NULL DEFAULT 'PENDING',
    role             VARCHAR(30)  NOT NULL DEFAULT 'USER',
    status           VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',
    created_at       TIMESTAMP    NOT NULL DEFAULT now(),
    updated_at       TIMESTAMP    NOT NULL DEFAULT now()
);