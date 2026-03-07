-- V3: Create academic_records and subject_scores tables
CREATE TABLE student_schema.academic_records (
    id               UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    student_id       UUID           NOT NULL REFERENCES student_schema.students(id),
    academic_year    INTEGER        NOT NULL,
    class_grade      INTEGER        NOT NULL CHECK (class_grade BETWEEN 10 AND 12),
    board            VARCHAR(20)    NOT NULL,
    percentage_score NUMERIC(5, 2)  NOT NULL CHECK (percentage_score BETWEEN 0 AND 100),
    cgpa             NUMERIC(4, 2),
    remarks          TEXT,
    version          BIGINT         NOT NULL DEFAULT 0,
    created_at       TIMESTAMPTZ    NOT NULL DEFAULT NOW(),
    deleted_at       TIMESTAMPTZ
);

CREATE INDEX idx_academic_records_student_id  ON student_schema.academic_records (student_id);
CREATE INDEX idx_academic_records_deleted_at  ON student_schema.academic_records (deleted_at);

CREATE TABLE student_schema.subject_scores (
    id                 UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    academic_record_id UUID          NOT NULL REFERENCES student_schema.academic_records(id),
    subject_name       VARCHAR(100)  NOT NULL,
    subject_code       VARCHAR(20),
    marks_obtained     INTEGER       NOT NULL,
    total_marks        INTEGER       NOT NULL,
    percentage         NUMERIC(5, 2) NOT NULL,
    created_at         TIMESTAMPTZ   NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_subject_scores_academic_record_id ON student_schema.subject_scores (academic_record_id);
