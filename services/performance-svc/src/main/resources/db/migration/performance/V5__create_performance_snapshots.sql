-- V5: Create performance_snapshots table and convert to TimescaleDB hypertable

CREATE TABLE performance_schema.performance_snapshots (
    id                          UUID         NOT NULL DEFAULT uuid_generate_v4(),
    student_id                  UUID         NOT NULL,
    enrollment_id               UUID         NOT NULL,
    ers_score                   NUMERIC(5,2) NOT NULL,
    theta                       NUMERIC(5,4) NOT NULL DEFAULT 0.0000,
    percentile                  NUMERIC(5,2),
    risk_level                  VARCHAR(10)  NOT NULL DEFAULT 'GREEN',
    dropout_risk_score          NUMERIC(4,3) NOT NULL DEFAULT 0.000,
    total_study_minutes_today   INTEGER      NOT NULL DEFAULT 0,
    mock_tests_this_week        INTEGER      NOT NULL DEFAULT 0,
    snapshot_at                 TIMESTAMPTZ  NOT NULL,
    created_at                  TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    CONSTRAINT pk_performance_snapshots PRIMARY KEY (id, snapshot_at)
);

-- Convert to TimescaleDB hypertable partitioned by snapshot_at
SELECT create_hypertable('performance_schema.performance_snapshots', 'snapshot_at');

-- BRIN index for time-range queries on hypertable
CREATE INDEX brin_performance_snapshots_snapshot_at
    ON performance_schema.performance_snapshots USING BRIN (snapshot_at);

CREATE INDEX idx_performance_snapshots_student_id
    ON performance_schema.performance_snapshots (student_id, snapshot_at DESC);
