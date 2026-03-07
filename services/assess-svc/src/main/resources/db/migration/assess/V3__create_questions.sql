-- V3__create_questions.sql
CREATE TABLE assess_schema.questions (
    id              UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    exam_id         UUID         NOT NULL REFERENCES assess_schema.exams(id),
    question_text   TEXT         NOT NULL,
    options_json    TEXT         NOT NULL,
    correct_answer  INT          NOT NULL CHECK (correct_answer >= 0),
    explanation     TEXT,
    marks           NUMERIC(6,2) NOT NULL CHECK (marks > 0),
    difficulty      DOUBLE PRECISION NOT NULL DEFAULT 0.0,
    discrimination  DOUBLE PRECISION NOT NULL DEFAULT 1.0,
    guessing_param  DOUBLE PRECISION NOT NULL DEFAULT 0.0,
    -- Future: embedding vector(1536) -- stored as TEXT until pgvector is configured
    embedding_json  TEXT,
    version         BIGINT       NOT NULL DEFAULT 0,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    deleted_at      TIMESTAMPTZ
);

CREATE INDEX idx_questions_exam_id ON assess_schema.questions(exam_id) WHERE deleted_at IS NULL;

CREATE TRIGGER trg_questions_updated_at
    BEFORE UPDATE ON assess_schema.questions
    FOR EACH ROW EXECUTE FUNCTION assess_schema.set_updated_at();
