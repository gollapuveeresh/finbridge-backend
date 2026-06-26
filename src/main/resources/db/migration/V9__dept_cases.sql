-- Department service cases (tax / investment / insurance / wealth). The polymorphic,
-- stage-specific workflow payload is stored in the JSONB `data` column.
CREATE TABLE IF NOT EXISTS dept_cases (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    case_id       VARCHAR(30) UNIQUE,
    department    VARCHAR(50) NOT NULL,
    client_id     UUID REFERENCES users(id),
    consultant_id UUID REFERENCES users(id),
    lead_id       UUID REFERENCES leads(id),
    stage         VARCHAR(50) NOT NULL DEFAULT 'document_collection',
    invoice_id    UUID REFERENCES invoices(id),
    data          JSONB NOT NULL DEFAULT '{}',
    is_active     BOOLEAN NOT NULL DEFAULT TRUE,
    created_at    TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at    TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_dept_cases_department ON dept_cases(department, is_active);
CREATE INDEX IF NOT EXISTS idx_dept_cases_consultant ON dept_cases(consultant_id);
