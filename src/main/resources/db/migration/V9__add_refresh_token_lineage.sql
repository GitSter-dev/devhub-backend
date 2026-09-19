ALTER TABLE refresh_tokens ADD COLUMN parent_id UUID;
ALTER TABLE refresh_tokens ADD COLUMN issued_for_request VARCHAR(64);

CREATE INDEX refresh_tokens_parent_idx ON refresh_tokens (parent_id);
