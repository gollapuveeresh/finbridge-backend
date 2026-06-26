-- Reconstructed from live DB (original file was lost from the repo).
-- Adds the email-verified flag on B2B organization users. Idempotent so it is a no-op where already applied.
ALTER TABLE organization_users ADD COLUMN IF NOT EXISTS is_email_verified boolean NOT NULL DEFAULT false;
