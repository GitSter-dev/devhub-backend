CREATE TABLE outbox_events
(
    id              UUID PRIMARY KEY,
    type            VARCHAR(50) NOT NULL,
    payload         JSONB       NOT NULL,
    status          VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    attempts        INT         NOT NULL DEFAULT 0,
    next_attempt_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    last_error      TEXT,
    processed_at    TIMESTAMPTZ,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX outbox_events_pending_idx ON outbox_events (next_attempt_at) WHERE status = 'PENDING';
