CREATE TABLE topics
(
    slug VARCHAR(40) PRIMARY KEY,
    name VARCHAR(60) NOT NULL
);

INSERT INTO topics (slug, name)
VALUES ('ai', 'AI'),
       ('databases', 'Databases'),
       ('design', 'Design'),
       ('devops', 'DevOps'),
       ('game-dev', 'Game dev'),
       ('go', 'Go'),
       ('kotlin', 'Kotlin'),
       ('open-source', 'Open source'),
       ('react-native', 'React Native'),
       ('rust', 'Rust'),
       ('security', 'Security'),
       ('typescript', 'TypeScript');

CREATE TABLE user_topics
(
    user_id    UUID        NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    topic_slug VARCHAR(40) NOT NULL REFERENCES topics (slug),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (user_id, topic_slug)
);

CREATE INDEX user_topics_topic_idx ON user_topics (topic_slug);
