-- V4: In-app notifications for leave-request lifecycle events.
CREATE TABLE notifications (
    id                BIGSERIAL PRIMARY KEY,
    user_id           BIGINT      NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    leave_request_id  BIGINT      REFERENCES leave_requests (id) ON DELETE CASCADE,
    event_type        VARCHAR(30) NOT NULL,
    message           TEXT        NOT NULL,
    is_read           BOOLEAN     NOT NULL DEFAULT FALSE,
    created_at        TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    read_at           TIMESTAMPTZ,
    CONSTRAINT chk_notifications_event_type CHECK (event_type IN
        ('CREATED', 'UPDATED', 'APPROVED', 'REJECTED', 'CANCELLED'))
);

CREATE INDEX idx_notifications_user_unread ON notifications (user_id, is_read);
