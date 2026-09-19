ALTER TABLE users
    ADD COLUMN bio                 VARCHAR(160),
    ADD COLUMN github_username     VARCHAR(39),
    ADD COLUMN website_url         VARCHAR(200),
    ADD COLUMN username_changed_at TIMESTAMPTZ;

CREATE TABLE held_usernames
(
    username_lower VARCHAR(30) PRIMARY KEY,
    user_id        UUID        NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    held_until     TIMESTAMPTZ NOT NULL
);

CREATE INDEX held_usernames_user_idx ON held_usernames (user_id);

CREATE EXTENSION IF NOT EXISTS pg_trgm;
CREATE EXTENSION IF NOT EXISTS unaccent;

CREATE FUNCTION immutable_unaccent(value TEXT) RETURNS TEXT
    LANGUAGE sql IMMUTABLE PARALLEL SAFE STRICT
AS
$$
SELECT public.unaccent('public.unaccent'::regdictionary, value)
$$;

CREATE INDEX users_username_trgm_idx ON users USING gin (lower(username) gin_trgm_ops);
CREATE INDEX users_display_name_trgm_idx ON users USING gin (lower(immutable_unaccent(display_name)) gin_trgm_ops);
