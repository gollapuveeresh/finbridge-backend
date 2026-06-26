-- Notes attached to a loan case (consultant CRM notes during the workflow).
CREATE TABLE IF NOT EXISTS loan_case_notes (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    loan_case_id UUID NOT NULL REFERENCES loan_cases(id) ON DELETE CASCADE,
    text         TEXT NOT NULL,
    added_by     VARCHAR(255),
    added_at     TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_loan_case_notes_case ON loan_case_notes(loan_case_id);
