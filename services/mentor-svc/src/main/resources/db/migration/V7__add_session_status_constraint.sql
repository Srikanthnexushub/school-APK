-- V7: Add CHECK constraint for SessionStatus enum values
-- Enum values verified from MentorSession.java: SCHEDULED, IN_PROGRESS, COMPLETED, CANCELLED, NO_SHOW

ALTER TABLE mentor_schema.mentor_sessions
    DROP CONSTRAINT IF EXISTS mentor_sessions_status_check;

ALTER TABLE mentor_schema.mentor_sessions
    ADD CONSTRAINT mentor_sessions_status_check
    CHECK (status IN ('SCHEDULED', 'IN_PROGRESS', 'COMPLETED', 'CANCELLED', 'NO_SHOW'));
