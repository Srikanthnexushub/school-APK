-- V5__add_mfa_fields.sql
-- Adds TOTP-based MFA columns to the users table.
-- mfa_enabled  : whether 2FA is active for this account
-- totp_secret  : Base32-encoded TOTP secret (null when MFA is disabled)

ALTER TABLE auth_schema.users
    ADD COLUMN IF NOT EXISTS mfa_enabled BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS totp_secret VARCHAR(64);
