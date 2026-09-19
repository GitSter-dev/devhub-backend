CREATE TABLE devices
(
    id                UUID PRIMARY KEY,
    user_id           UUID         NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    installation_id   VARCHAR(64)  NOT NULL,
    session_family_id UUID,
    platform          VARCHAR(10)  NOT NULL,
    push_token        TEXT,
    device_name       VARCHAR(100),
    app_version       VARCHAR(30),
    last_seen_at      TIMESTAMPTZ  NOT NULL,
    revoked_at        TIMESTAMPTZ,
    revocation_reason VARCHAR(20),
    created_at        TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at        TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT devices_installation_id_key UNIQUE (installation_id)
);

CREATE UNIQUE INDEX devices_push_token_key ON devices (push_token) WHERE push_token IS NOT NULL;
CREATE INDEX devices_active_user_idx ON devices (user_id) WHERE revoked_at IS NULL;
CREATE INDEX devices_session_family_idx ON devices (session_family_id);
