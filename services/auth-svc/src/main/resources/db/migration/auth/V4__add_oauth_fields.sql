-- V4__add_oauth_fields.sql
-- Adds provider-based OAuth2 sign-in support (Google, etc.)
-- Allows nullable password_hash for OAuth-only accounts.

ALTER TABLE auth_schema.users
    ADD COLUMN IF NOT EXISTS provider       VARCHAR(30),
    ADD COLUMN IF NOT EXISTS provider_id    VARCHAR(255),
    ADD COLUMN IF NOT EXISTS email_verified BOOLEAN NOT NULL DEFAULT FALSE;

-- Back-fill: existing email/password accounts have verified emails after OTP step
UPDATE auth_schema.users
SET email_verified = TRUE
WHERE status = 'ACTIVE' AND provider IS NULL;

-- Partial unique index: one account per (provider, provider_id) pair
CREATE UNIQUE INDEX IF NOT EXISTS idx_users_provider_id
    ON auth_schema.users (provider, provider_id)
    WHERE provider IS NOT NULL AND deleted_at IS NULL;

-- Allow null password_hash for OAuth accounts (they have no password)
ALTER TABLE auth_schema.users
    ALTER COLUMN password_hash DROP NOT NULL;

COMMENT ON COLUMN auth_schema.users.provider IS 'e.g. GOOGLE — null for email/password accounts';
COMMENT ON COLUMN auth_schema.users.provider_id IS 'External provider subject (sub) claim';
COMMENT ON COLUMN auth_schema.users.email_verified IS 'true for OAuth users and OTP-verified users';
