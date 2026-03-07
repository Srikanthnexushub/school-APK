-- V3__create_student_links.sql
-- Parent-student relationship. Status REVOKED = logically deleted.

CREATE TABLE parent_schema.student_links (
    id           UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    parent_id    UUID        NOT NULL REFERENCES parent_schema.parent_profiles(id),
    student_id   UUID        NOT NULL,
    student_name TEXT        NOT NULL,
    center_id    UUID        NOT NULL,
    status       TEXT        NOT NULL DEFAULT 'ACTIVE',
    version      BIGINT      NOT NULL DEFAULT 0,
    created_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT chk_link_status CHECK (status IN ('ACTIVE', 'REVOKED'))
);

-- A student can be linked to a parent only once (regardless of status — re-link creates new row after revoke)
-- Unique on ACTIVE links only: same parent cannot have two ACTIVE links to same student
CREATE UNIQUE INDEX uq_student_links_active
    ON parent_schema.student_links(parent_id, student_id)
    WHERE status = 'ACTIVE';

-- Lookup index for parent's student list
CREATE INDEX idx_student_links_parent_id
    ON parent_schema.student_links(parent_id);

-- Lookup index for finding all parents of a student
CREATE INDEX idx_student_links_student_active
    ON parent_schema.student_links(student_id)
    WHERE status = 'ACTIVE';

CREATE TRIGGER trg_student_links_updated_at
    BEFORE UPDATE ON parent_schema.student_links
    FOR EACH ROW EXECUTE FUNCTION parent_schema.set_updated_at();
