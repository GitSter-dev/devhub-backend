CREATE TABLE conversations
(
    id              UUID PRIMARY KEY,
    kind            VARCHAR(10) NOT NULL,
    title           VARCHAR(80),
    direct_key      VARCHAR(73) UNIQUE,
    created_by      UUID        NOT NULL REFERENCES users (id) ON DELETE RESTRICT,
    last_seq        BIGINT      NOT NULL DEFAULT 0,
    last_message_at TIMESTAMPTZ,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT conversations_kind CHECK (kind IN ('DIRECT', 'GROUP')),
    CONSTRAINT conversations_direct_key CHECK ((kind = 'DIRECT') = (direct_key IS NOT NULL))
);

CREATE TABLE conversation_members
(
    conversation_id UUID        NOT NULL REFERENCES conversations (id) ON DELETE RESTRICT,
    user_id         UUID        NOT NULL REFERENCES users (id) ON DELETE RESTRICT,
    role            VARCHAR(10) NOT NULL,
    status          VARCHAR(10) NOT NULL,
    joined_at       TIMESTAMPTZ NOT NULL DEFAULT now(),
    delivered_seq   BIGINT      NOT NULL DEFAULT 0,
    read_seq        BIGINT      NOT NULL DEFAULT 0,
    PRIMARY KEY (conversation_id, user_id),
    CONSTRAINT conversation_members_role CHECK (role IN ('OWNER', 'MEMBER')),
    CONSTRAINT conversation_members_status CHECK (status IN ('ACTIVE', 'REQUEST', 'DECLINED', 'LEFT')),
    CONSTRAINT conversation_members_read_within_delivered CHECK (read_seq <= delivered_seq)
);

CREATE INDEX conversation_members_user_idx ON conversation_members (user_id, status);

CREATE TABLE messages
(
    id                UUID PRIMARY KEY,
    conversation_id   UUID        NOT NULL REFERENCES conversations (id) ON DELETE RESTRICT,
    seq               BIGINT      NOT NULL,
    sender_id         UUID REFERENCES users (id) ON DELETE RESTRICT,
    kind              VARCHAR(10) NOT NULL,
    body              VARCHAR(4000),
    code              TEXT,
    code_language     VARCHAR(20),
    reply_to_id       UUID REFERENCES messages (id) ON DELETE RESTRICT,
    system_type       VARCHAR(20),
    system_actor_id   UUID REFERENCES users (id) ON DELETE RESTRICT,
    system_target_id  UUID REFERENCES users (id) ON DELETE RESTRICT,
    client_message_id UUID,
    deleted_at        TIMESTAMPTZ,
    created_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT messages_seq_key UNIQUE (conversation_id, seq),
    CONSTRAINT messages_client_key UNIQUE (sender_id, client_message_id),
    CONSTRAINT messages_kind CHECK (kind IN ('TEXT', 'SYSTEM')),
    CONSTRAINT messages_code_length CHECK (char_length(code) <= 4000),
    CONSTRAINT messages_text_has_content CHECK (kind = 'SYSTEM' OR deleted_at IS NOT NULL OR body IS NOT NULL OR code IS NOT NULL),
    CONSTRAINT messages_system_shape CHECK ((kind = 'SYSTEM') = (system_type IS NOT NULL AND sender_id IS NULL))
);
