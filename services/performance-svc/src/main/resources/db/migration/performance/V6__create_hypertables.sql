-- V6: Activate TimescaleDB hypertables for time-series performance data.
-- TimescaleDB is optional — skipped gracefully when not available (e.g. local dev).

DO $$
DECLARE
    timescale_installed BOOLEAN;
BEGIN
    SELECT EXISTS (
        SELECT 1 FROM pg_extension WHERE extname = 'timescaledb'
    ) INTO timescale_installed;

    IF NOT timescale_installed THEN
        RAISE NOTICE 'timescaledb not installed — skipping hypertable creation';
        RETURN;
    END IF;

    -- readiness_scores: drop single-column PK, recreate with computed_at
    ALTER TABLE performance_schema.readiness_scores DROP CONSTRAINT IF EXISTS pk_readiness_scores;
    ALTER TABLE performance_schema.readiness_scores
        ADD CONSTRAINT pk_readiness_scores PRIMARY KEY (id, computed_at);

    PERFORM create_hypertable(
        'performance_schema.readiness_scores',
        'computed_at',
        if_not_exists => TRUE,
        migrate_data => TRUE
    );

    -- weak_area_records: drop PK if exists without time column
    IF EXISTS (
        SELECT 1 FROM information_schema.table_constraints
        WHERE table_schema = 'performance_schema'
          AND table_name = 'weak_area_records'
          AND constraint_type = 'PRIMARY KEY'
    ) THEN
        ALTER TABLE performance_schema.weak_area_records DROP CONSTRAINT IF EXISTS pk_weak_area_records;
    END IF;

    IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = 'performance_schema'
          AND table_name = 'weak_area_records'
          AND column_name = 'detected_at'
    ) THEN
        PERFORM create_hypertable(
            'performance_schema.weak_area_records',
            'detected_at',
            if_not_exists => TRUE,
            migrate_data => TRUE
        );
    END IF;
END $$;

-- Standard view for daily readiness averages (continuous aggregate requires TimescaleDB Enterprise)
CREATE OR REPLACE VIEW performance_schema.daily_readiness_avg AS
SELECT
    date_trunc('day', computed_at)     AS day,
    student_id,
    AVG(ers_score)::NUMERIC(5,2)       AS avg_ers_score,
    COUNT(*)::INT                      AS snapshot_count,
    MAX(ers_score)                     AS peak_ers_score,
    MIN(ers_score)                     AS low_ers_score
FROM performance_schema.readiness_scores
GROUP BY date_trunc('day', computed_at), student_id;

COMMENT ON VIEW performance_schema.daily_readiness_avg IS
    'Daily avg ERS score per student. Standard view backed by TimescaleDB hypertable.';
