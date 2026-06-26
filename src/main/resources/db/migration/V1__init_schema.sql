-- V1__init_schema.sql

CREATE TABLE IF NOT EXISTS users (
    id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name              VARCHAR(255) NOT NULL,
    email             VARCHAR(255) NOT NULL UNIQUE,
    password          VARCHAR(255) NOT NULL,
    role              VARCHAR(50)  NOT NULL DEFAULT 'client',
    department        VARCHAR(50),
    phone             VARCHAR(50),
    company_name      VARCHAR(255),
    is_active         BOOLEAN NOT NULL DEFAULT TRUE,
    is_email_verified BOOLEAN NOT NULL DEFAULT FALSE,
    created_at        TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at        TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS leads (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    lead_id             VARCHAR(20) UNIQUE,
    name                VARCHAR(255) NOT NULL,
    email               VARCHAR(255) NOT NULL,
    phone               VARCHAR(50),
    income              NUMERIC(15,2),
    requirement         TEXT,
    budget              NUMERIC(15,2),
    source              VARCHAR(50)  NOT NULL DEFAULT 'website_form',
    status              VARCHAR(50)  NOT NULL DEFAULT 'new',
    priority            VARCHAR(20)  NOT NULL DEFAULT 'warm',
    score               INTEGER      NOT NULL DEFAULT 0,
    department          VARCHAR(50),
    service_type        VARCHAR(100),
    assigned_consultant UUID REFERENCES users(id),
    assigned_admin      UUID REFERENCES users(id),
    crm_owner           UUID REFERENCES users(id),
    converted_client_id UUID REFERENCES users(id),
    follow_up_date      TIMESTAMPTZ,
    last_contacted_at   TIMESTAMPTZ,
    tags                TEXT[]       DEFAULT '{}',
    is_active           BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at          TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS lead_notes (
    id        UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    lead_id   UUID NOT NULL REFERENCES leads(id) ON DELETE CASCADE,
    text      TEXT NOT NULL,
    added_by  VARCHAR(255),
    added_at  TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS proposals (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    lead_id         UUID REFERENCES leads(id),
    client_id       UUID REFERENCES users(id),
    consultant_id   UUID NOT NULL REFERENCES users(id),
    department      VARCHAR(50) NOT NULL,
    title           VARCHAR(255) NOT NULL,
    summary         TEXT,
    details         JSONB        DEFAULT '{}',
    status          VARCHAR(50)  NOT NULL DEFAULT 'draft',
    client_feedback TEXT,
    invoice_id      UUID,
    case_id         UUID,
    case_model      VARCHAR(50),
    valid_until     TIMESTAMPTZ,
    is_active       BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS proposal_documents (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    proposal_id UUID NOT NULL REFERENCES proposals(id) ON DELETE CASCADE,
    name        VARCHAR(255),
    url         TEXT
);

CREATE TABLE IF NOT EXISTS loans (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id             UUID NOT NULL REFERENCES users(id),
    loan_number         VARCHAR(50) NOT NULL UNIQUE,
    loan_type           VARCHAR(100) NOT NULL,
    lender_name         VARCHAR(255) NOT NULL,
    principal_amount    NUMERIC(15,2) NOT NULL,
    outstanding_balance NUMERIC(15,2) NOT NULL,
    interest_rate       NUMERIC(6,2)  NOT NULL,
    tenure_months       INTEGER       NOT NULL,
    monthly_emi         NUMERIC(15,2) NOT NULL,
    start_date          DATE NOT NULL,
    end_date            DATE NOT NULL,
    status              VARCHAR(30) NOT NULL DEFAULT 'Active',
    notes               TEXT DEFAULT '',
    is_active           BOOLEAN NOT NULL DEFAULT TRUE,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS investments (
    id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id          UUID NOT NULL REFERENCES users(id),
    investment_type  VARCHAR(50) NOT NULL,
    amount_invested  NUMERIC(15,2) NOT NULL,
    current_value    NUMERIC(15,2) NOT NULL,
    purchase_date    DATE NOT NULL,
    risk_level       VARCHAR(20) NOT NULL,
    notes            TEXT DEFAULT '',
    is_active        BOOLEAN NOT NULL DEFAULT TRUE,
    created_at       TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at       TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS consultations (
    id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    client_id      UUID NOT NULL REFERENCES users(id),
    consultant_id  UUID REFERENCES users(id),
    department     VARCHAR(50) NOT NULL,
    category       VARCHAR(255) NOT NULL,
    status         VARCHAR(30) NOT NULL DEFAULT 'pending',
    client_notes   TEXT DEFAULT '',
    confirmed_date VARCHAR(50) DEFAULT '',
    confirmed_time VARCHAR(50) DEFAULT '',
    meeting_link   TEXT DEFAULT '',
    created_at     TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at     TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS financial_profiles (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id             UUID NOT NULL UNIQUE REFERENCES users(id),
    assigned_consultant UUID REFERENCES users(id),
    annual_income       NUMERIC(15,2) DEFAULT 0,
    monthly_income      NUMERIC(15,2) DEFAULT 0,
    monthly_expenses    NUMERIC(15,2) DEFAULT 0,
    savings             NUMERIC(15,2) DEFAULT 0,
    emergency_fund      NUMERIC(15,2) DEFAULT 0,
    credit_score        INTEGER DEFAULT 600,
    total_loan_amount   NUMERIC(15,2) DEFAULT 0,
    monthly_emi         NUMERIC(15,2) DEFAULT 0,
    business_name       VARCHAR(255) DEFAULT '',
    business_type       VARCHAR(255) DEFAULT '',
    annual_revenue      NUMERIC(15,2) DEFAULT 0,
    annual_expenses     NUMERIC(15,2) DEFAULT 0,
    years_in_business   INTEGER DEFAULT 0,
    current_investments NUMERIC(15,2) DEFAULT 0,
    risk_tolerance      VARCHAR(20) DEFAULT 'Medium',
    investment_goals    TEXT[] DEFAULT '{}',
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS existing_loans (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    profile_id      UUID NOT NULL REFERENCES financial_profiles(id) ON DELETE CASCADE,
    loan_type       VARCHAR(100) NOT NULL,
    amount          NUMERIC(15,2) DEFAULT 0,
    monthly_payment NUMERIC(15,2) DEFAULT 0
);

CREATE TABLE IF NOT EXISTS notifications (
    id         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id    UUID NOT NULL REFERENCES users(id),
    type       VARCHAR(100) DEFAULT 'consultation',
    title      VARCHAR(255) NOT NULL,
    message    TEXT NOT NULL,
    metadata   JSONB DEFAULT '{}',
    is_read    BOOLEAN NOT NULL DEFAULT FALSE,
    is_active  BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS invoices (
    id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    invoice_number VARCHAR(50) UNIQUE,
    client_id      UUID NOT NULL REFERENCES users(id),
    consultant_id  UUID NOT NULL REFERENCES users(id),
    proposal_id    UUID REFERENCES proposals(id),
    department     VARCHAR(50) NOT NULL,
    service_title  VARCHAR(255) NOT NULL,
    subtotal       NUMERIC(15,2) NOT NULL,
    tax            NUMERIC(15,2) DEFAULT 0,
    tax_percent    NUMERIC(5,2)  DEFAULT 18,
    total_amount   NUMERIC(15,2) NOT NULL,
    currency       VARCHAR(10)   DEFAULT 'INR',
    status         VARCHAR(30)   NOT NULL DEFAULT 'draft',
    due_date       TIMESTAMPTZ,
    paid_at        TIMESTAMPTZ,
    notes          TEXT,
    is_active      BOOLEAN NOT NULL DEFAULT TRUE,
    created_at     TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at     TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS invoice_line_items (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    invoice_id  UUID NOT NULL REFERENCES invoices(id) ON DELETE CASCADE,
    description TEXT NOT NULL,
    amount      NUMERIC(15,2) NOT NULL
);

CREATE TABLE IF NOT EXISTS payments (
    id                 UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    client_id          UUID NOT NULL REFERENCES users(id),
    invoice_id         UUID NOT NULL REFERENCES invoices(id),
    amount             NUMERIC(15,2) NOT NULL,
    currency           VARCHAR(10)   DEFAULT 'INR',
    gateway            VARCHAR(30)   DEFAULT 'razorpay',
    gateway_order_id   VARCHAR(255),
    gateway_payment_id VARCHAR(255),
    gateway_signature  TEXT,
    status             VARCHAR(30)   NOT NULL DEFAULT 'created',
    method             VARCHAR(30)   DEFAULT '',
    paid_at            TIMESTAMPTZ,
    refund_id          VARCHAR(255),
    refunded_at        TIMESTAMPTZ,
    refund_amount      NUMERIC(15,2),
    notes              TEXT,
    created_at         TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at         TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS loan_products (
    id                   UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    bank_name            VARCHAR(255) NOT NULL,
    loan_type            VARCHAR(100) NOT NULL,
    min_credit_score     INTEGER NOT NULL,
    max_credit_score     INTEGER NOT NULL,
    min_monthly_income   NUMERIC(15,2) NOT NULL,
    max_loan_amount      NUMERIC(15,2) NOT NULL,
    interest_rate        NUMERIC(6,2)  NOT NULL,
    processing_fee       NUMERIC(6,2)  NOT NULL,
    tenure_months        INTEGER NOT NULL,
    description          TEXT DEFAULT '',
    features             TEXT[] DEFAULT '{}',
    eligibility_criteria TEXT DEFAULT '',
    bank_logo            TEXT DEFAULT '',
    official_website     TEXT DEFAULT '',
    pre_approved         BOOLEAN NOT NULL DEFAULT FALSE,
    featured             BOOLEAN NOT NULL DEFAULT FALSE,
    is_active            BOOLEAN NOT NULL DEFAULT TRUE,
    created_at           TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at           TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS loan_cases (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    case_id             VARCHAR(20) UNIQUE,
    client_id           UUID NOT NULL REFERENCES users(id),
    consultant_id       UUID NOT NULL REFERENCES users(id),
    lead_id             UUID REFERENCES leads(id),
    stage               VARCHAR(50) NOT NULL DEFAULT 'document_collection',
    loan_type           VARCHAR(100) DEFAULT '',
    requested_amount    NUMERIC(15,2) DEFAULT 0,
    approved_amount     NUMERIC(15,2),
    interest_rate       NUMERIC(6,2),
    tenure_months       INTEGER,
    monthly_emi         NUMERIC(15,2),
    bank_name           VARCHAR(255) DEFAULT '',
    disbursed_date      DATE,
    disbursed_amount    NUMERIC(15,2),
    credit_score        INTEGER,
    dti                 NUMERIC(6,2),
    ltv                 NUMERIC(6,2),
    eligible            BOOLEAN,
    analyst_note        TEXT DEFAULT '',
    recommended_bank    VARCHAR(255) DEFAULT '',
    recommended_rate    NUMERIC(6,2),
    recommended_tenure  INTEGER,
    recommended_emi     NUMERIC(15,2),
    recommendation_note TEXT DEFAULT '',
    sent_to_client      BOOLEAN DEFAULT FALSE,
    client_decision     VARCHAR(30) DEFAULT 'Pending',
    client_feedback     TEXT DEFAULT '',
    decided_at          TIMESTAMPTZ,
    application_ref     VARCHAR(255) DEFAULT '',
    submitted_date      DATE,
    bank_status         VARCHAR(50) DEFAULT 'Not Submitted',
    sanctioned_at       TIMESTAMPTZ,
    bank_remarks        TEXT DEFAULT '',
    proposal_id         UUID REFERENCES proposals(id),
    invoice_id          UUID REFERENCES invoices(id),
    is_active           BOOLEAN NOT NULL DEFAULT TRUE,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS loan_case_documents (
    id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    loan_case_id   UUID NOT NULL REFERENCES loan_cases(id) ON DELETE CASCADE,
    name           VARCHAR(255) NOT NULL,
    category       VARCHAR(50) DEFAULT 'Other',
    status         VARCHAR(30) DEFAULT 'Pending',
    uploaded_at    TIMESTAMPTZ,
    rejection_note TEXT DEFAULT ''
);

CREATE TABLE IF NOT EXISTS emi_schedule (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    loan_case_id UUID NOT NULL REFERENCES loan_cases(id) ON DELETE CASCADE,
    month        VARCHAR(20) NOT NULL,
    due_date     DATE NOT NULL,
    amount       NUMERIC(15,2) NOT NULL,
    paid_date    DATE,
    status       VARCHAR(20) DEFAULT 'Pending',
    penalty      NUMERIC(15,2) DEFAULT 0
);

CREATE INDEX IF NOT EXISTS idx_leads_status       ON leads(status);
CREATE INDEX IF NOT EXISTS idx_leads_department   ON leads(department);
CREATE INDEX IF NOT EXISTS idx_loans_user         ON loans(user_id, is_active);
CREATE INDEX IF NOT EXISTS idx_investments_user   ON investments(user_id, is_active);
CREATE INDEX IF NOT EXISTS idx_notifications_user ON notifications(user_id, is_read);
CREATE INDEX IF NOT EXISTS idx_proposals_client   ON proposals(client_id);
CREATE INDEX IF NOT EXISTS idx_loan_cases_client  ON loan_cases(client_id);
