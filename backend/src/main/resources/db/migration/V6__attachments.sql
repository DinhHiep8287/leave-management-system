-- V6: Local-only leave request attachments.
CREATE TABLE attachments (
    id                 BIGSERIAL PRIMARY KEY,
    leave_request_id   BIGINT      NOT NULL REFERENCES leave_requests(id) ON DELETE CASCADE,
    uploaded_by        BIGINT      NOT NULL REFERENCES users(id),
    original_filename  TEXT        NOT NULL,
    stored_key         TEXT        NOT NULL UNIQUE,
    content_type       VARCHAR(100) NOT NULL,
    size_bytes         BIGINT      NOT NULL,
    created_at         TIMESTAMPTZ NOT NULL,
    updated_at         TIMESTAMPTZ NOT NULL,
    created_by         VARCHAR(100),
    updated_by         VARCHAR(100),
    CONSTRAINT ck_attachments_content_type CHECK (content_type IN ('application/pdf', 'image/jpeg', 'image/png')),
    CONSTRAINT ck_attachments_size CHECK (size_bytes > 0 AND size_bytes <= 5242880)
);

CREATE INDEX idx_attachments_leave_request ON attachments(leave_request_id);
CREATE INDEX idx_attachments_uploaded_by ON attachments(uploaded_by);
