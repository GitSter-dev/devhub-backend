CREATE TABLE one_time_codes
(
    id                UUID PRIMARY KEY,
    user_id           UUID         NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    purpose           VARCHAR(30)  NOT NULL,
    code_hash         VARCHAR(255) NOT NULL,
    expires_at        TIMESTAMPTZ  NOT NULL,
    attempts          INT          NOT NULL DEFAULT 0,
    issued_at         TIMESTAMPTZ  NOT NULL,
    window_started_at TIMESTAMPTZ  NOT NULL,
    issues_in_window  INT          NOT NULL,
    created_at        TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at        TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT one_time_codes_user_purpose_key UNIQUE (user_id, purpose)
);

DROP TABLE email_verification_codes;
