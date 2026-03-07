-- V5__create_schedules_table.sql
CREATE TABLE center_schema.schedules (
    id              UUID        NOT NULL DEFAULT gen_random_uuid(),
    batch_id        UUID        NOT NULL,
    center_id       UUID        NOT NULL,
    day_of_week     VARCHAR(10) NOT NULL,
    start_time      TIME        NOT NULL,
    end_time        TIME        NOT NULL,
    room            VARCHAR(100),
    effective_from  DATE        NOT NULL,
    effective_to    DATE,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    version         BIGINT      NOT NULL DEFAULT 0,

    CONSTRAINT pk_schedules          PRIMARY KEY (id),
    CONSTRAINT fk_schedules_batch    FOREIGN KEY (batch_id) REFERENCES center_schema.batches (id),
    CONSTRAINT fk_schedules_center   FOREIGN KEY (center_id) REFERENCES center_schema.centers (id),
    CONSTRAINT chk_schedules_time    CHECK (start_time < end_time),
    CONSTRAINT chk_schedules_day     CHECK (day_of_week IN (
        'MONDAY','TUESDAY','WEDNESDAY','THURSDAY','FRIDAY','SATURDAY','SUNDAY'
    ))
);

CREATE INDEX idx_schedules_batch_id  ON center_schema.schedules (batch_id);
CREATE INDEX idx_schedules_center_id ON center_schema.schedules (center_id);

CREATE TRIGGER trg_schedules_updated_at
    BEFORE UPDATE ON center_schema.schedules
    FOR EACH ROW EXECUTE FUNCTION center_schema.set_updated_at();
