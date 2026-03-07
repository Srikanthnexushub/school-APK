-- V6__create_submission_answers.sql
-- Immutable: once written, no UPDATE ever occurs
CREATE TABLE assess_schema.submission_answers (
    id              UUID             PRIMARY KEY DEFAULT gen_random_uuid(),
    submission_id   UUID             NOT NULL REFERENCES assess_schema.submissions(id),
    question_id     UUID             NOT NULL REFERENCES assess_schema.questions(id),
    selected_option INT              NOT NULL CHECK (selected_option >= 0),
    is_correct      BOOLEAN          NOT NULL,
    marks_awarded   DOUBLE PRECISION NOT NULL DEFAULT 0,
    answered_at     TIMESTAMPTZ      NOT NULL DEFAULT now(),
    created_at      TIMESTAMPTZ      NOT NULL DEFAULT now()
    -- NO updated_at: immutable record
);

-- A student can answer each question only once per submission
CREATE UNIQUE INDEX uq_submission_answers
    ON assess_schema.submission_answers(submission_id, question_id);

CREATE INDEX idx_submission_answers_submission_id
    ON assess_schema.submission_answers(submission_id);

-- BRIN: append-only table
CREATE INDEX idx_submission_answers_created_brin
    ON assess_schema.submission_answers USING BRIN(created_at);
