CREATE TABLE notifications
(
    id           UUID PRIMARY KEY,
    recipient_id UUID        NOT NULL REFERENCES users (id) ON DELETE RESTRICT,
    type         VARCHAR(20) NOT NULL,
    group_key    VARCHAR(80) NOT NULL,
    subject_id   UUID,
    seen_at      TIMESTAMPTZ,
    push_due_at  TIMESTAMPTZ,
    pushed_count INT         NOT NULL DEFAULT 0,
    pushed_at    TIMESTAMPTZ,
    created_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT notifications_type CHECK (type IN ('POST_LIKED', 'POST_REPLIED', 'NEW_FOLLOWER', 'FOLLOWED_POSTED', 'MESSAGE_REQUEST'))
);

CREATE UNIQUE INDEX notifications_open_group_key ON notifications (recipient_id, group_key) WHERE seen_at IS NULL;
CREATE INDEX notifications_group_idx ON notifications (recipient_id, group_key);
CREATE INDEX notifications_recipient_recent_idx ON notifications (recipient_id, updated_at DESC, id DESC);
CREATE INDEX notifications_push_due_idx ON notifications (push_due_at) WHERE push_due_at IS NOT NULL;

CREATE TABLE notification_actors
(
    notification_id UUID        NOT NULL REFERENCES notifications (id) ON DELETE CASCADE,
    actor_id        UUID        NOT NULL REFERENCES users (id) ON DELETE RESTRICT,
    subject_id      UUID,
    acted_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (notification_id, actor_id)
);

CREATE INDEX notification_actors_subject_idx ON notification_actors (subject_id);
