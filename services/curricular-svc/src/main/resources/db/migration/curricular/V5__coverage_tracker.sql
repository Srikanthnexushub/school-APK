CREATE TABLE IF NOT EXISTS curricular_schema.coverage_logs (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    teacher_id      UUID        NOT NULL,
    center_id       UUID        NOT NULL,
    batch_id        UUID,
    topic_id        UUID        NOT NULL REFERENCES curricular_schema.topics(id),
    taught_on       DATE        NOT NULL,
    delivery_method VARCHAR(30) NOT NULL DEFAULT 'LECTURE' CHECK (delivery_method IN ('LECTURE','PRACTICAL','ONLINE_CLASS','FLIPPED','WORKSHOP','SELF_STUDY')),
    duration_mins   INTEGER     NOT NULL DEFAULT 60,
    notes           TEXT,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_coverage_teacher ON curricular_schema.coverage_logs(teacher_id);
CREATE INDEX IF NOT EXISTS idx_coverage_center ON curricular_schema.coverage_logs(center_id);
CREATE INDEX IF NOT EXISTS idx_coverage_topic ON curricular_schema.coverage_logs(topic_id);
CREATE INDEX IF NOT EXISTS idx_coverage_taught_on ON curricular_schema.coverage_logs(taught_on);
