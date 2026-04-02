CREATE TABLE IF NOT EXISTS curricular_schema.extracurricular_activities (
    id                   UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    center_id            UUID         NOT NULL,
    name                 VARCHAR(200) NOT NULL,
    description          TEXT,
    category             VARCHAR(40)  NOT NULL CHECK (category IN ('SPORTS','ARTS','MUSIC','DANCE','DRAMA','CODING_CLUB','DEBATE','SCIENCE_CLUB','COMMUNITY_SERVICE','LEADERSHIP','CULTURAL_EVENTS','OTHER')),
    max_participants     INTEGER      NOT NULL DEFAULT 30,
    supervisor_id        UUID,
    schedule_description VARCHAR(500),
    is_active            BOOLEAN      NOT NULL DEFAULT TRUE,
    created_by           UUID         NOT NULL,
    created_at           TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at           TIMESTAMPTZ
);

CREATE TABLE IF NOT EXISTS curricular_schema.activity_enrollments (
    id          UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    activity_id UUID        NOT NULL REFERENCES curricular_schema.extracurricular_activities(id),
    student_id  UUID        NOT NULL,
    enrolled_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    status      VARCHAR(20) NOT NULL DEFAULT 'ENROLLED' CHECK (status IN ('ENROLLED','WAITLISTED','COMPLETED','WITHDRAWN')),
    withdrawn_at TIMESTAMPTZ,
    UNIQUE (activity_id, student_id)
);

CREATE TABLE IF NOT EXISTS curricular_schema.activity_sessions (
    id               UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    activity_id      UUID        NOT NULL REFERENCES curricular_schema.extracurricular_activities(id),
    session_date     DATE        NOT NULL,
    venue            VARCHAR(200),
    duration_mins    INTEGER     NOT NULL DEFAULT 60,
    conducted_by_id  UUID,
    notes            TEXT,
    created_at       TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS curricular_schema.activity_achievements (
    id               UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    student_id       UUID        NOT NULL,
    activity_id      UUID        NOT NULL REFERENCES curricular_schema.extracurricular_activities(id),
    achievement_type VARCHAR(30) NOT NULL CHECK (achievement_type IN ('PARTICIPATION','MERIT','EXCELLENCE','GOLD','SILVER','BRONZE','SPECIAL')),
    title            VARCHAR(200),
    description      TEXT,
    issued_at        DATE        NOT NULL,
    issued_by_id     UUID        NOT NULL,
    created_at       TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_activities_center ON curricular_schema.extracurricular_activities(center_id);
CREATE INDEX IF NOT EXISTS idx_enrollments_activity ON curricular_schema.activity_enrollments(activity_id);
CREATE INDEX IF NOT EXISTS idx_enrollments_student ON curricular_schema.activity_enrollments(student_id);
CREATE INDEX IF NOT EXISTS idx_sessions_activity ON curricular_schema.activity_sessions(activity_id);
CREATE INDEX IF NOT EXISTS idx_achievements_student ON curricular_schema.activity_achievements(student_id);
CREATE INDEX IF NOT EXISTS idx_achievements_activity ON curricular_schema.activity_achievements(activity_id);
