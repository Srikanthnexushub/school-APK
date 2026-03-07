-- V7__create_grades.sql
CREATE TABLE assess_schema.grades (
    id              UUID             PRIMARY KEY DEFAULT gen_random_uuid(),
    submission_id   UUID             NOT NULL UNIQUE REFERENCES assess_schema.submissions(id),
    student_id      UUID             NOT NULL,
    exam_id         UUID             NOT NULL REFERENCES assess_schema.exams(id),
    batch_id        UUID             NOT NULL,
    center_id       UUID             NOT NULL,
    percentage      DOUBLE PRECISION NOT NULL,
    letter_grade    TEXT             NOT NULL,
    passed          BOOLEAN          NOT NULL,
    version         BIGINT           NOT NULL DEFAULT 0,
    created_at      TIMESTAMPTZ      NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ      NOT NULL DEFAULT now(),
    CONSTRAINT chk_letter_grade CHECK (letter_grade IN ('A', 'B', 'C', 'D', 'F'))
);

CREATE INDEX idx_grades_exam_id    ON assess_schema.grades(exam_id);
CREATE INDEX idx_grades_student_id ON assess_schema.grades(student_id);
CREATE INDEX idx_grades_batch_id   ON assess_schema.grades(batch_id);

CREATE TRIGGER trg_grades_updated_at
    BEFORE UPDATE ON assess_schema.grades
    FOR EACH ROW EXECUTE FUNCTION assess_schema.set_updated_at();
