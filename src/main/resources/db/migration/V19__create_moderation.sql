CREATE TABLE moderation_cases
(
    id                UUID PRIMARY KEY,
    target_type       VARCHAR(10) NOT NULL,
    target_id         UUID        NOT NULL,
    owner_id          UUID        NOT NULL REFERENCES users (id) ON DELETE RESTRICT,
    status            VARCHAR(12) NOT NULL DEFAULT 'OPEN',
    reporter_count    INT         NOT NULL DEFAULT 0,
    severity          INT         NOT NULL DEFAULT 0,
    auto_hidden_at    TIMESTAMPTZ,
    first_reported_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    last_reported_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    resolved_by       UUID REFERENCES users (id) ON DELETE RESTRICT,
    resolved_at       TIMESTAMPTZ,
    created_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT moderation_cases_target_key UNIQUE (target_type, target_id),
    CONSTRAINT moderation_cases_target_type CHECK (target_type IN ('POST', 'MESSAGE', 'USER')),
    CONSTRAINT moderation_cases_status CHECK (status IN ('OPEN', 'ACTIONED', 'DISMISSED'))
);

CREATE INDEX moderation_cases_queue_idx ON moderation_cases (status, severity DESC, last_reported_at DESC);

CREATE TABLE reports
(
    id              UUID PRIMARY KEY,
    case_id         UUID        NOT NULL REFERENCES moderation_cases (id) ON DELETE RESTRICT,
    reporter_id     UUID        NOT NULL REFERENCES users (id) ON DELETE RESTRICT,
    target_type     VARCHAR(10) NOT NULL,
    target_id       UUID        NOT NULL,
    target_owner_id UUID        NOT NULL REFERENCES users (id) ON DELETE RESTRICT,
    reason          VARCHAR(20) NOT NULL,
    note            VARCHAR(500),
    snapshot        TEXT        NOT NULL,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT reports_once_per_reporter UNIQUE (reporter_id, target_type, target_id),
    CONSTRAINT reports_target_type CHECK (target_type IN ('POST', 'MESSAGE', 'USER')),
    CONSTRAINT reports_reason CHECK (reason IN ('SPAM', 'HARASSMENT', 'HATE', 'SEXUAL', 'VIOLENCE', 'SELF_HARM',
                                                'IMPERSONATION', 'OTHER'))
);

CREATE INDEX reports_case_idx ON reports (case_id, created_at DESC);
CREATE INDEX reports_reporter_idx ON reports (reporter_id, created_at DESC);

CREATE TABLE moderation_actions
(
    id             UUID PRIMARY KEY,
    case_id        UUID REFERENCES moderation_cases (id) ON DELETE RESTRICT,
    moderator_id   UUID        NOT NULL REFERENCES users (id) ON DELETE RESTRICT,
    action         VARCHAR(20) NOT NULL,
    target_user_id UUID REFERENCES users (id) ON DELETE RESTRICT,
    note           VARCHAR(500),
    acts_until     TIMESTAMPTZ,
    created_at     TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT moderation_actions_action CHECK (action IN ('DISMISS', 'REMOVE_CONTENT', 'WARN', 'SUSPEND', 'BAN',
                                                           'RESTORE', 'REINSTATE'))
);

CREATE INDEX moderation_actions_case_idx ON moderation_actions (case_id, created_at DESC);
CREATE INDEX moderation_actions_user_idx ON moderation_actions (target_user_id, created_at DESC);

ALTER TABLE posts
    ADD COLUMN removed_at TIMESTAMPTZ,
    ADD COLUMN hidden_at  TIMESTAMPTZ;

ALTER TABLE messages
    ADD COLUMN removed_at TIMESTAMPTZ;

ALTER TABLE notifications
    DROP CONSTRAINT notifications_type,
    ADD CONSTRAINT notifications_type CHECK (type IN ('POST_LIKED', 'POST_REPLIED', 'NEW_FOLLOWER', 'FOLLOWED_POSTED',
                                                      'MESSAGE_REQUEST', 'REPORT_RESOLVED'));
