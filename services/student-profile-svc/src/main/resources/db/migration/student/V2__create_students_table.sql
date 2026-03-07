-- V2: Create students table
CREATE TABLE student_schema.students (
    id           UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id      UUID        NOT NULL,
    first_name   VARCHAR(100) NOT NULL,
    last_name    VARCHAR(100) NOT NULL,
    email        VARCHAR(255) NOT NULL,
    phone        VARCHAR(20),
    gender       VARCHAR(10)  NOT NULL,
    date_of_birth DATE        NOT NULL,
    city         VARCHAR(100),
    state        VARCHAR(100),
    pincode      VARCHAR(10),
    current_board VARCHAR(20) NOT NULL,
    current_class INTEGER     NOT NULL CHECK (current_class BETWEEN 10 AND 13),
    stream       VARCHAR(20),
    target_year  INTEGER,
    status       VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',
    version      BIGINT       NOT NULL DEFAULT 0,
    created_at   TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at   TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    deleted_at   TIMESTAMPTZ,
    CONSTRAINT uq_students_email UNIQUE (email)
);

CREATE INDEX idx_students_user_id   ON student_schema.students (user_id);
CREATE INDEX idx_students_email     ON student_schema.students (email);
CREATE INDEX idx_students_status    ON student_schema.students (status);
CREATE INDEX idx_students_deleted_at ON student_schema.students (deleted_at);
