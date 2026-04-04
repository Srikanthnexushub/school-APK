-- V7: Create immutable audit events log table.
-- Persists every event published to the audit-immutable Kafka topic so that
-- auth-svc has a queryable, append-only audit trail without relying on Kafka
-- log retention as the sole durability mechanism.
CREATE TABLE IF NOT EXISTS auth_schema.audit_events_log (
    id           UUID                     PRIMARY KEY DEFAULT gen_random_uuid(),
    event_type   VARCHAR(100)             NOT NULL,
    user_id      UUID,
    action       VARCHAR(200)             NOT NULL,
    details      JSONB,
    ip_address   VARCHAR(50),
    timestamp    TIMESTAMP WITH TIME ZONE NOT NULL,
    created_at   TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_audit_events_log_user_id
    ON auth_schema.audit_events_log(user_id);

CREATE INDEX IF NOT EXISTS idx_audit_events_log_timestamp
    ON auth_schema.audit_events_log(timestamp);
