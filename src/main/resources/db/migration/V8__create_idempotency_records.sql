CREATE TABLE idempotency_records
(
    id              UUID PRIMARY KEY,
    idempotency_key VARCHAR(200) NOT NULL,
    endpoint        VARCHAR(200) NOT NULL,
    request_hash    VARCHAR(64)  NOT NULL,
    status          VARCHAR(20)  NOT NULL,
    response_status INT,
    response_body   TEXT,
    expires_at      TIMESTAMPTZ  NOT NULL,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT idempotency_records_key_endpoint_key UNIQUE (idempotency_key, endpoint)
);
