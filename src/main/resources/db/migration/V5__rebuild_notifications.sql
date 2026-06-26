-- V5__rebuild_notifications.sql
-- The notifications table was created manually without the full schema.
-- Drop and recreate with all columns the Hibernate entity requires.

DROP TABLE IF EXISTS notifications CASCADE;

CREATE TABLE notifications (
    id         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id    UUID NOT NULL REFERENCES users(id),
    type       VARCHAR(100) NOT NULL DEFAULT 'consultation',
    title      VARCHAR(255) NOT NULL,
    message    TEXT         NOT NULL,
    metadata   JSONB        DEFAULT '{}',
    is_read    BOOLEAN      NOT NULL DEFAULT FALSE,
    is_active  BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_notifications_user ON notifications(user_id, is_read);
