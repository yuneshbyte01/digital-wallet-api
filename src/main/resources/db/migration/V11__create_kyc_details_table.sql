CREATE TABLE kyc_details (
    id                    UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id               UUID         NOT NULL UNIQUE
     REFERENCES users(id) ON DELETE CASCADE,
    date_of_birth         DATE,
    address               TEXT,
    father_name           VARCHAR(100),
    mother_name           VARCHAR(100),
    grandfather_name      VARCHAR(100),
    marital_status        VARCHAR(20)  CHECK (marital_status IN ('SINGLE', 'MARRIED')),
    spouse_name           VARCHAR(100),
    spouse_phone          VARCHAR(10),
    document_type         VARCHAR(50)  CHECK (document_type IN (
                                                             'CITIZENSHIP',
                                                             'PASSPORT',
                                                             'DRIVING_LICENSE',
                                                             'NATIONAL_ID'
     )),
    document_id           VARCHAR(100),
    document_issue_date   DATE,
    document_issued_place VARCHAR(150),
    profile_picture_path  VARCHAR(500),
    document_picture_path VARCHAR(500),
    submitted_at          TIMESTAMP,
    reviewed_at           TIMESTAMP,
    reviewed_by           UUID         REFERENCES users(id) ON DELETE SET NULL,
    rejection_reason      TEXT,
    created_at            TIMESTAMP    NOT NULL DEFAULT now(),
    updated_at            TIMESTAMP    NOT NULL DEFAULT now()
);

CREATE INDEX idx_kyc_details_user_id ON kyc_details(user_id);