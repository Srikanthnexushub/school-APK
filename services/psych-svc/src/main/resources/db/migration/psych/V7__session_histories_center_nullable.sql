-- V7: Allow self-service profiles (no center) to start sessions.
-- V6 made psych_profiles.center_id nullable but missed session_histories.center_id.
ALTER TABLE psych_schema.session_histories ALTER COLUMN center_id DROP NOT NULL;
