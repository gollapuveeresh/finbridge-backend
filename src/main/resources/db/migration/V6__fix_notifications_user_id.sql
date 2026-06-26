-- V6__fix_notifications_user_id.sql
-- Add user_id FK column that Hibernate entity requires

ALTER TABLE notifications
    ADD COLUMN IF NOT EXISTS user_id UUID REFERENCES users(id);

-- Make it NOT NULL only if there are no rows, otherwise leave nullable
-- (existing rows if any would need a valid user_id — leaving nullable is safe for dev)
