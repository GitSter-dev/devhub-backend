CREATE TABLE community_rules
(
    id           UUID PRIMARY KEY,
    community_id UUID         NOT NULL REFERENCES communities (id) ON DELETE CASCADE,
    position     INTEGER      NOT NULL,
    title        VARCHAR(80)  NOT NULL,
    body         VARCHAR(300),
    CONSTRAINT community_rules_position UNIQUE (community_id, position)
);

CREATE TABLE community_bans
(
    community_id UUID        NOT NULL REFERENCES communities (id) ON DELETE CASCADE,
    user_id      UUID        NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    banned_by    UUID        NOT NULL REFERENCES users (id) ON DELETE RESTRICT,
    reason       VARCHAR(500),
    expires_at   TIMESTAMPTZ,
    created_at   TIMESTAMPTZ NOT NULL,
    PRIMARY KEY (community_id, user_id)
);

ALTER TABLE posts
    ADD COLUMN pinned_at     TIMESTAMPTZ,
    ADD COLUMN removal_scope VARCHAR(20),
    ADD CONSTRAINT posts_removal_scope CHECK (removal_scope IN ('PLATFORM', 'COMMUNITY')),
    ADD CONSTRAINT posts_removal_scope_matches CHECK ((removal_scope IS NULL) = (removed_at IS NULL));

UPDATE posts SET removal_scope = 'PLATFORM' WHERE removed_at IS NOT NULL;

CREATE INDEX posts_community_pinned_idx ON posts (community_id, pinned_at DESC) WHERE pinned_at IS NOT NULL;

ALTER TABLE communities
    ADD COLUMN owner_id UUID REFERENCES users (id) ON DELETE RESTRICT;

UPDATE communities c
SET owner_id = (SELECT m.user_id FROM community_members m WHERE m.community_id = c.id AND m.role = 'OWNER');

ALTER TABLE communities
    ALTER COLUMN owner_id SET NOT NULL;

ALTER TABLE moderation_cases
    ADD COLUMN community_id UUID REFERENCES communities (id) ON DELETE RESTRICT,
    DROP CONSTRAINT moderation_cases_target_type,
    ADD CONSTRAINT moderation_cases_target_type CHECK (target_type IN ('POST', 'MESSAGE', 'USER', 'COMMUNITY'));

CREATE INDEX moderation_cases_community_queue_idx
    ON moderation_cases (community_id, status, severity DESC, last_reported_at DESC)
    WHERE community_id IS NOT NULL;

ALTER TABLE reports
    DROP CONSTRAINT reports_target_type,
    ADD CONSTRAINT reports_target_type CHECK (target_type IN ('POST', 'MESSAGE', 'USER', 'COMMUNITY'));

ALTER TABLE moderation_actions
    ADD COLUMN community_id UUID REFERENCES communities (id) ON DELETE RESTRICT,
    ADD COLUMN target_post_id UUID REFERENCES posts (id) ON DELETE RESTRICT,
    DROP CONSTRAINT moderation_actions_action,
    ADD CONSTRAINT moderation_actions_action CHECK (action IN ('DISMISS', 'REMOVE_CONTENT', 'WARN', 'SUSPEND', 'BAN',
                                                               'RESTORE', 'REINSTATE', 'COMMUNITY_BAN',
                                                               'COMMUNITY_UNBAN'));

CREATE INDEX moderation_actions_community_idx ON moderation_actions (community_id, created_at DESC, id DESC)
    WHERE community_id IS NOT NULL;

ALTER TABLE notifications
    ALTER COLUMN type TYPE VARCHAR(30),
    DROP CONSTRAINT notifications_type,
    ADD CONSTRAINT notifications_type CHECK (type IN ('POST_LIKED', 'POST_REPLIED', 'NEW_FOLLOWER', 'FOLLOWED_POSTED',
                                                      'MESSAGE_REQUEST', 'REPORT_RESOLVED',
                                                      'COMMUNITY_JOIN_REQUESTED', 'COMMUNITY_JOIN_APPROVED',
                                                      'COMMUNITY_POST_REMOVED'));
