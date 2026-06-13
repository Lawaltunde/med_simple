-- =============================================================
-- MedSimple — Auth: refresh tokens
-- V4__auth_refresh_tokens.sql
--
-- Access tokens are stateless JWTs (not stored). Refresh tokens are opaque,
-- stored hashed, single-use (rotated on refresh), and revocable on logout.
-- =============================================================

CREATE TABLE refresh_tokens (
    id          UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id     UUID         NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    -- SHA-256 hash of the opaque token — the raw value is never stored.
    token_hash  VARCHAR(255) NOT NULL UNIQUE,
    expires_at  TIMESTAMPTZ  NOT NULL,
    revoked     BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_refresh_tokens_user      ON refresh_tokens(user_id);
CREATE INDEX idx_refresh_tokens_hash      ON refresh_tokens(token_hash);
-- Active (unrevoked, unexpired) tokens are the common lookup.
CREATE INDEX idx_refresh_tokens_active    ON refresh_tokens(user_id) WHERE revoked = FALSE;
