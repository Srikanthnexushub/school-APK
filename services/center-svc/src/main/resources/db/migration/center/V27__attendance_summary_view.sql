-- V27__attendance_summary_view.sql
-- Aggregate view for attendance reports. Threshold filtering (e.g. < 75%)
-- is applied at the query layer, not here, so callers control the cutoff.
CREATE VIEW center_schema.attendance_summary AS
SELECT
    bm.batch_id,
    bm.center_id,
    bm.student_id,
    bm.student_name,
    COUNT(a.id)                                                                        AS total_days,
    COUNT(a.id) FILTER (WHERE a.status = 'PRESENT')                                   AS present_days,
    COUNT(a.id) FILTER (WHERE a.status = 'ABSENT')                                    AS absent_days,
    COUNT(a.id) FILTER (WHERE a.status = 'LATE')                                      AS late_days,
    COUNT(a.id) FILTER (WHERE a.status = 'EXCUSED')                                   AS excused_days,
    ROUND(
        100.0 * COUNT(a.id) FILTER (WHERE a.status IN ('PRESENT', 'LATE'))
        / NULLIF(COUNT(a.id), 0),
        2
    )                                                                                  AS attendance_pct
FROM center_schema.batch_members bm
LEFT JOIN center_schema.attendance a
       ON a.batch_id   = bm.batch_id
      AND a.student_id = bm.student_id
WHERE bm.withdrawn_at IS NULL
GROUP BY bm.batch_id, bm.center_id, bm.student_id, bm.student_name;
