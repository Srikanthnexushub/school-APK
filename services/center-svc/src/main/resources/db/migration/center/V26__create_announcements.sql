-- V26__create_announcements.sql
-- Targeted announcements from CENTER_ADMIN / TEACHER to specific audiences.
-- Distinct from banners (platform marketing). Delivered via notification-svc Kafka.
CREATE TABLE center_schema.announcements (
    id           UUID         NOT NULL DEFAULT gen_random_uuid(),
    center_id    UUID         NOT NULL,
    title        VARCHAR(300) NOT NULL,
    body         TEXT         NOT NULL,
    sent_by_id   UUID         NOT NULL,
    sent_by_role VARCHAR(50)  NOT NULL,
    target_type  VARCHAR(30)  NOT NULL,
    target_id    UUID,
    target_role  VARCHAR(30),
    scheduled_at TIMESTAMPTZ,
    sent_at      TIMESTAMPTZ,
    expires_at   TIMESTAMPTZ,
    created_at   TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT pk_announcements   PRIMARY KEY (id),
    CONSTRAINT chk_ann_target     CHECK (target_type IN ('BATCH', 'CENTER', 'ROLE', 'ALL')),
    CONSTRAINT chk_ann_role       CHECK (target_role IS NULL OR
                                        target_role IN ('STUDENT', 'PARENT', 'TEACHER', 'ALL'))
);

CREATE INDEX idx_announcements_center    ON center_schema.announcements(center_id);
CREATE INDEX idx_announcements_scheduled ON center_schema.announcements(scheduled_at)
    WHERE sent_at IS NULL;
