CREATE TABLE email_verification_codes
(
    user_id    UUID PRIMARY KEY REFERENCES users (id) ON DELETE CASCADE,
    code_hash  VARCHAR(255) NOT NULL,
    expires_at TIMESTAMPTZ  NOT NULL,
    attempts   INT          NOT NULL DEFAULT 0,
    issued_at  TIMESTAMPTZ  NOT NULL
);
