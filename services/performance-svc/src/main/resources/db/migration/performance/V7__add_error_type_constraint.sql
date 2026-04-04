-- V7: Add CHECK constraint for ErrorType enum values
-- Enum values verified from ErrorType.java:
--   CONCEPTUAL_GAP, CALCULATION_ERROR, TIME_PRESSURE, TOPIC_UNFAMILIARITY, SILLY_MISTAKE

ALTER TABLE performance_schema.weak_area_records
    DROP CONSTRAINT IF EXISTS weak_area_records_error_type_check;

ALTER TABLE performance_schema.weak_area_records
    ADD CONSTRAINT weak_area_records_error_type_check
    CHECK (primary_error_type IN (
        'CONCEPTUAL_GAP',
        'CALCULATION_ERROR',
        'TIME_PRESSURE',
        'TOPIC_UNFAMILIARITY',
        'SILLY_MISTAKE'
    ));
