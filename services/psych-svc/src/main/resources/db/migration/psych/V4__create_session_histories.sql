CREATE TABLE psych_schema.session_histories (
    id           UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    profile_id   UUID         NOT NULL REFERENCES psych_schema.psych_profiles(id),
    student_id   UUID         NOT NULL,
    center_id    UUID         NOT NULL,
    session_type TEXT         NOT NULL,
    scheduled_at TIMESTAMPTZ  NOT NULL,
    started_at   TIMESTAMPTZ,
    completed_at TIMESTAMPTZ,
    notes        TEXT,
    status       TEXT         NOT NULL DEFAULT 'SCHEDULED',
    version      BIGINT       NOT NULL DEFAULT 0,
    created_at   TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at   TIMESTAMPTZ  NOT NULL DEFAULT now(),
    deleted_at   TIMESTAMPTZ,
    CONSTRAINT chk_sh_session_type CHECK (session_type IN ('INITIAL', 'PERIODIC', 'TRIGGERED')),
    CONSTRAINT chk_sh_status       CHECK (status IN ('SCHEDULED', 'IN_PROGRESS', 'COMPLETED', 'CANCELLED'))
);

CREATE INDEX idx_sh_profile_id ON psych_schema.session_histories(profile_id) WHERE deleted_at IS NULL;
CREATE INDEX idx_sh_student_id ON psych_schema.session_histories(student_id) WHERE deleted_at IS NULL;
CREATE INDEX idx_sh_status     ON psych_schema.session_histories(status)     WHERE deleted_at IS NULL;

-- BRIN index for append-heavy table
CREATE INDEX idx_sh_created_brin ON psych_schema.session_histories USING BRIN(created_at);

CREATE TRIGGER trg_session_histories_updated_at
    BEFORE UPDATE ON psych_schema.session_histories
    FOR EACH ROW EXECUTE FUNCTION psych_schema.set_updated_at();
