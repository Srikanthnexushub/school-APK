-- Reminders for Academic Plans — persisted per-student, acknowledged on dismiss
CREATE TABLE aimentor_schema.study_plan_reminders (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    study_plan_id UUID NOT NULL REFERENCES aimentor_schema.study_plans(id) ON DELETE CASCADE,
    student_id UUID NOT NULL,
    reminder_type VARCHAR(30) NOT NULL DEFAULT 'REVIEW_REMINDER',
    remind_at TIMESTAMPTZ NOT NULL,
    title VARCHAR(255) NOT NULL,
    message TEXT,
    acknowledged BOOLEAN NOT NULL DEFAULT false,
    acknowledged_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_reminders_student_due
    ON aimentor_schema.study_plan_reminders(student_id, remind_at)
    WHERE acknowledged = false;
CREATE INDEX idx_reminders_plan
    ON aimentor_schema.study_plan_reminders(study_plan_id);
