CREATE TABLE posts
(
    id            UUID PRIMARY KEY,
    author_id     UUID         NOT NULL REFERENCES users (id) ON DELETE RESTRICT,
    body          VARCHAR(500),
    code          TEXT,
    code_language VARCHAR(20),
    parent_id     UUID REFERENCES posts (id) ON DELETE RESTRICT,
    root_id       UUID REFERENCES posts (id) ON DELETE RESTRICT,
    deleted_at    TIMESTAMPTZ,
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT posts_thread_consistent CHECK ((parent_id IS NULL) = (root_id IS NULL)),
    CONSTRAINT posts_live_has_content CHECK (deleted_at IS NOT NULL OR body IS NOT NULL OR code IS NOT NULL),
    CONSTRAINT posts_code_length CHECK (char_length(code) <= 4000)
);

CREATE INDEX posts_author_recent_idx ON posts (author_id, created_at DESC, id DESC);
CREATE INDEX posts_parent_idx ON posts (parent_id, created_at, id);
CREATE INDEX posts_live_top_level_recent_idx ON posts (created_at DESC, id DESC)
    WHERE parent_id IS NULL AND deleted_at IS NULL;

CREATE TABLE post_likes
(
    post_id    UUID        NOT NULL REFERENCES posts (id) ON DELETE CASCADE,
    user_id    UUID        NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (post_id, user_id)
);

CREATE INDEX post_likes_user_idx ON post_likes (user_id);
