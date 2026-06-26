-- V4__fix_notifications_columns.sql
-- Add missing columns to notifications table that Hibernate entity expects

ALTER TABLE notifications
    ADD COLUMN IF NOT EXISTS is_active   BOOLEAN     NOT NULL DEFAULT TRUE,
    ADD COLUMN IF NOT EXISTS updated_at  TIMESTAMPTZ NOT NULL DEFAULT NOW();
