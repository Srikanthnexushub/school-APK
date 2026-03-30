-- V30__backfill_batch_enrolled_count.sql
-- Backfills enrolled_count on the batches table from actual active batch_members rows.
-- Required because BatchMemberService never called batch.incrementEnrollment() before Fix #157.
UPDATE center_schema.batches b
SET enrolled_count = (
    SELECT COUNT(*)
    FROM center_schema.batch_members m
    WHERE m.batch_id = b.id
      AND m.withdrawn_at IS NULL
),
    updated_at = now()
WHERE EXISTS (
    SELECT 1 FROM center_schema.batch_members m WHERE m.batch_id = b.id
);
