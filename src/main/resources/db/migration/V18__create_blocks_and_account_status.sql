CREATE TABLE blocks
(
    blocker_id UUID        NOT NULL REFERENCES users (id) ON DELETE RESTRICT,
    blocked_id UUID        NOT NULL REFERENCES users (id) ON DELETE RESTRICT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (blocker_id, blocked_id),
    CONSTRAINT blocks_not_self CHECK (blocker_id <> blocked_id)
);

CREATE INDEX blocks_blocked_idx ON blocks (blocked_id);

ALTER TABLE users
    ADD COLUMN suspended_until TIMESTAMPTZ,
    ADD COLUMN banned_at       TIMESTAMPTZ,
    ADD COLUMN deactivated_at  TIMESTAMPTZ,
    ADD COLUMN deleted_at      TIMESTAMPTZ;
