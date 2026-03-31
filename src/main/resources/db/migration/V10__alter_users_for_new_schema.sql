ALTER TABLE users RENAME COLUMN pin_attempts TO failed_login_attempts;

ALTER TABLE users ADD COLUMN last_login_at      TIMESTAMP;
ALTER TABLE users ADD COLUMN account_locked_until TIMESTAMP;

UPDATE users
SET kyc_status = 'NOT_SUBMITTED'
WHERE kyc_status = 'PENDING';

ALTER TABLE users ALTER COLUMN kyc_status SET DEFAULT 'NOT_SUBMITTED';

ALTER TABLE users DROP COLUMN IF EXISTS kyc_doc_type;
ALTER TABLE users DROP COLUMN IF EXISTS kyc_doc_number;

CREATE INDEX IF NOT EXISTS idx_users_email      ON users(email);
CREATE INDEX IF NOT EXISTS idx_users_phone      ON users(phone);
CREATE INDEX IF NOT EXISTS idx_users_kyc_status ON users(kyc_status);