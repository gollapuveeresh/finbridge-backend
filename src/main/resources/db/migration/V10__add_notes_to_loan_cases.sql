-- Reconstructed from live DB (original file was lost from the repo).
-- Adds the JSONB notes array on loan_cases. Idempotent so it is a no-op where already applied.
ALTER TABLE loan_cases ADD COLUMN IF NOT EXISTS notes jsonb NOT NULL DEFAULT '[]'::jsonb;
