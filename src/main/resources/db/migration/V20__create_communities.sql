CREATE TABLE communities
(
    id                UUID PRIMARY KEY,
    slug              VARCHAR(30)  NOT NULL,
    name              VARCHAR(60)  NOT NULL,
    description       VARCHAR(500),
    join_policy       VARCHAR(20)  NOT NULL,
    slow_mode_seconds INTEGER      NOT NULL DEFAULT 0,
    member_count      INTEGER      NOT NULL DEFAULT 0,
    created_by        UUID         NOT NULL REFERENCES users (id) ON DELETE RESTRICT,
    removed_at        TIMESTAMPTZ,
    created_at        TIMESTAMPTZ  NOT NULL,
    updated_at        TIMESTAMPTZ  NOT NULL,
    CONSTRAINT communities_slug_format CHECK (slug ~ '^[a-z0-9]([a-z0-9-]*[a-z0-9])?$' AND length(slug) BETWEEN 3 AND 30),
    CONSTRAINT communities_join_policy CHECK (join_policy IN ('OPEN', 'RESTRICTED')),
    CONSTRAINT communities_slow_mode_range CHECK (slow_mode_seconds BETWEEN 0 AND 3600),
    CONSTRAINT communities_member_count_non_negative CHECK (member_count >= 0)
);

CREATE UNIQUE INDEX communities_slug_idx ON communities (slug);
CREATE INDEX communities_name_trgm_idx ON communities USING gin (lower(immutable_unaccent(name)) gin_trgm_ops);
CREATE INDEX communities_popular_idx ON communities (member_count DESC, id) WHERE removed_at IS NULL;

CREATE TABLE community_topics
(
    community_id UUID        NOT NULL REFERENCES communities (id) ON DELETE CASCADE,
    topic_slug   VARCHAR(40) NOT NULL REFERENCES topics (slug),
    PRIMARY KEY (community_id, topic_slug)
);

CREATE INDEX community_topics_topic_idx ON community_topics (topic_slug);

CREATE TABLE community_members
(
    community_id   UUID        NOT NULL REFERENCES communities (id) ON DELETE CASCADE,
    user_id        UUID        NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    role           VARCHAR(20) NOT NULL,
    joined_at      TIMESTAMPTZ NOT NULL,
    last_posted_at TIMESTAMPTZ,
    PRIMARY KEY (community_id, user_id),
    CONSTRAINT community_members_role CHECK (role IN ('OWNER', 'MODERATOR', 'MEMBER'))
);

CREATE INDEX community_members_user_idx ON community_members (user_id, joined_at DESC);
CREATE UNIQUE INDEX community_members_one_owner_idx ON community_members (community_id) WHERE role = 'OWNER';

CREATE TABLE community_join_requests
(
    id           UUID PRIMARY KEY,
    community_id UUID        NOT NULL REFERENCES communities (id) ON DELETE CASCADE,
    user_id      UUID        NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    status       VARCHAR(20) NOT NULL,
    message      VARCHAR(300),
    decided_by   UUID REFERENCES users (id) ON DELETE SET NULL,
    decided_at   TIMESTAMPTZ,
    created_at   TIMESTAMPTZ NOT NULL,
    updated_at   TIMESTAMPTZ NOT NULL,
    CONSTRAINT community_join_requests_status CHECK (status IN ('PENDING', 'APPROVED', 'DECLINED'))
);

CREATE UNIQUE INDEX community_join_requests_one_pending_idx
    ON community_join_requests (community_id, user_id) WHERE status = 'PENDING';
CREATE INDEX community_join_requests_queue_idx
    ON community_join_requests (community_id, created_at, id) WHERE status = 'PENDING';

ALTER TABLE posts
    ADD COLUMN community_id UUID REFERENCES communities (id) ON DELETE RESTRICT;

CREATE INDEX posts_community_recent_idx ON posts (community_id, created_at DESC, id DESC)
    WHERE community_id IS NOT NULL AND parent_id IS NULL AND deleted_at IS NULL;
