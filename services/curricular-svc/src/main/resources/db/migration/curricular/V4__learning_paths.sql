CREATE TABLE IF NOT EXISTS curricular_schema.learning_paths (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    student_id      UUID        NOT NULL,
    subject_id      UUID        REFERENCES curricular_schema.subjects(id),
    grade_id        UUID        REFERENCES curricular_schema.grade_levels(id),
    computed_at     TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    next_review_at  TIMESTAMPTZ,
    mastery_source  VARCHAR(30) NOT NULL DEFAULT 'PERFORMANCE_SVC',
    version         BIGINT      NOT NULL DEFAULT 0
);

CREATE TABLE IF NOT EXISTS curricular_schema.learning_path_items (
    id             UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    path_id        UUID         NOT NULL REFERENCES curricular_schema.learning_paths(id) ON DELETE CASCADE,
    topic_id       UUID         NOT NULL REFERENCES curricular_schema.topics(id),
    sequence_order INTEGER      NOT NULL,
    urgency_score  NUMERIC(5,2) NOT NULL DEFAULT 0.00,
    status         VARCHAR(20)  NOT NULL DEFAULT 'PENDING' CHECK (status IN ('PENDING','IN_PROGRESS','COMPLETED','SKIPPED')),
    completed_at   TIMESTAMPTZ,
    UNIQUE (path_id, sequence_order)
);

CREATE INDEX IF NOT EXISTS idx_learning_paths_student ON curricular_schema.learning_paths(student_id);
CREATE INDEX IF NOT EXISTS idx_learning_paths_student_subject_grade ON curricular_schema.learning_paths(student_id, subject_id, grade_id);
CREATE INDEX IF NOT EXISTS idx_learning_path_items_path ON curricular_schema.learning_path_items(path_id);
