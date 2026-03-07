CREATE TABLE careeroracle_schema.college_predictions (
    id UUID PRIMARY KEY,
    student_id UUID NOT NULL,
    enrollment_id UUID NOT NULL,
    college_name VARCHAR(255) NOT NULL,
    course_name VARCHAR(255) NOT NULL,
    college_tier VARCHAR(20) NOT NULL,
    predicted_cutoff NUMERIC(6,2),
    student_predicted_score NUMERIC(6,2),
    admission_probability NUMERIC(5,2),
    generated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    version BIGINT NOT NULL DEFAULT 0
);
CREATE INDEX idx_college_pred_student ON careeroracle_schema.college_predictions(student_id);
