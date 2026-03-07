-- V7__create_attendance_table.sql
-- Attendance is immutable once inserted. No UPDATE trigger needed.
CREATE TABLE center_schema.attendance (
    id                    UUID        NOT NULL DEFAULT gen_random_uuid(),
    batch_id              UUID        NOT NULL,
    center_id             UUID        NOT NULL,
    student_id            UUID        NOT NULL,
    date                  DATE        NOT NULL,
    status                VARCHAR(20) NOT NULL,
    marked_by_teacher_id  UUID        NOT NULL,
    notes                 VARCHAR(500),
    created_at            TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    CONSTRAINT pk_attendance             PRIMARY KEY (id),
    CONSTRAINT fk_attendance_batch       FOREIGN KEY (batch_id) REFERENCES center_schema.batches (id),
    CONSTRAINT uq_attendance_student_batch_date UNIQUE (batch_id, student_id, date),
    CONSTRAINT chk_attendance_status     CHECK (status IN ('PRESENT','ABSENT','LATE','EXCUSED'))
);

CREATE INDEX idx_attendance_batch_date     ON center_schema.attendance (batch_id, date);
CREATE INDEX idx_attendance_student_batch  ON center_schema.attendance (student_id, batch_id);
CREATE INDEX idx_attendance_created_at_brin ON center_schema.attendance USING BRIN (created_at);
