-- V6__create_copilot_tables.sql
-- Parent Copilot conversation persistence tables.
-- Each conversation has an ordered list of messages (user / assistant turns).

CREATE TABLE IF NOT EXISTS parent_schema.copilot_conversations (
    id              BIGSERIAL       PRIMARY KEY,
    parent_id       VARCHAR(255)    NOT NULL,
    student_id      VARCHAR(255),
    title           VARCHAR(200),
    status          VARCHAR(20)     NOT NULL DEFAULT 'ACTIVE',
    created_at      TIMESTAMP       NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP,
    CONSTRAINT chk_copilot_conv_status CHECK (status IN ('ACTIVE', 'CLOSED'))
);

CREATE INDEX idx_copilot_conv_parent ON parent_schema.copilot_conversations(parent_id);
CREATE INDEX idx_copilot_conv_student ON parent_schema.copilot_conversations(student_id);

CREATE TABLE IF NOT EXISTS parent_schema.copilot_messages (
    id                  BIGSERIAL   PRIMARY KEY,
    conversation_id     BIGINT      NOT NULL REFERENCES parent_schema.copilot_conversations(id) ON DELETE CASCADE,
    role                VARCHAR(20) NOT NULL,
    content             TEXT        NOT NULL,
    sent_at             TIMESTAMP   NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_copilot_msg_role CHECK (role IN ('user', 'assistant'))
);

CREATE INDEX idx_copilot_msg_conv ON parent_schema.copilot_messages(conversation_id);
