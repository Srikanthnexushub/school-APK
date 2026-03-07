-- V8__create_content_items_table.sql
CREATE TABLE center_schema.content_items (
    id                    UUID          NOT NULL DEFAULT gen_random_uuid(),
    center_id             UUID          NOT NULL,
    batch_id              UUID,
    title                 VARCHAR(500)  NOT NULL,
    description           VARCHAR(2000),
    type                  VARCHAR(20)   NOT NULL,
    file_url              VARCHAR(2000) NOT NULL,
    file_size_bytes       BIGINT,
    uploaded_by_user_id   UUID          NOT NULL,
    status                VARCHAR(20)   NOT NULL DEFAULT 'AVAILABLE',
    created_at            TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    updated_at            TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    deleted_at            TIMESTAMPTZ,
    version               BIGINT        NOT NULL DEFAULT 0,

    CONSTRAINT pk_content_items        PRIMARY KEY (id),
    CONSTRAINT fk_content_center       FOREIGN KEY (center_id) REFERENCES center_schema.centers (id),
    CONSTRAINT fk_content_batch        FOREIGN KEY (batch_id)  REFERENCES center_schema.batches (id),
    CONSTRAINT chk_content_type        CHECK (type IN ('VIDEO','PDF','DOCUMENT','QUIZ_REF','LINK')),
    CONSTRAINT chk_content_status      CHECK (status IN ('PROCESSING','AVAILABLE','ARCHIVED'))
);

CREATE INDEX idx_content_center_id ON center_schema.content_items (center_id) WHERE deleted_at IS NULL;
CREATE INDEX idx_content_batch_id  ON center_schema.content_items (batch_id)  WHERE batch_id IS NOT NULL AND deleted_at IS NULL;
CREATE INDEX idx_content_type      ON center_schema.content_items (type)      WHERE deleted_at IS NULL;

CREATE TRIGGER trg_content_items_updated_at
    BEFORE UPDATE ON center_schema.content_items
    FOR EACH ROW EXECUTE FUNCTION center_schema.set_updated_at();
