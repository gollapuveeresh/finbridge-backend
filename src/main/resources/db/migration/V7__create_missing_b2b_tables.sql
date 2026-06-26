-- V7__create_missing_b2b_tables.sql
-- V3 failed mid-way; create ALL B2B tables in dependency order.

-- 1. Organizations (must exist before all others)
CREATE TABLE IF NOT EXISTS organizations (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    company_name    VARCHAR(255) NOT NULL,
    industry        VARCHAR(100),
    gstin           VARCHAR(20)  UNIQUE,
    cin             VARCHAR(25),
    pan             VARCHAR(12),
    annual_turnover NUMERIC(18,2),
    employee_count  INTEGER,
    address         TEXT,
    city            VARCHAR(100),
    state           VARCHAR(100),
    pincode         VARCHAR(10),
    website         TEXT,
    services        TEXT[]       DEFAULT '{}',
    status          VARCHAR(30)  NOT NULL DEFAULT 'pending',
    kyc_verified    BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

-- 2. Organization Users
CREATE TABLE IF NOT EXISTS organization_users (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id UUID NOT NULL REFERENCES organizations(id) ON DELETE CASCADE,
    name            VARCHAR(255) NOT NULL,
    email           VARCHAR(255) NOT NULL UNIQUE,
    password_hash   VARCHAR(255) NOT NULL,
    role            VARCHAR(50)  NOT NULL DEFAULT 'COMPANY_ADMIN',
    is_active       BOOLEAN      NOT NULL DEFAULT TRUE,
    last_login      TIMESTAMPTZ,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_org_users_org ON organization_users(organization_id);

-- 3. Organization Documents
CREATE TABLE IF NOT EXISTS organization_documents (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id UUID NOT NULL REFERENCES organizations(id) ON DELETE CASCADE,
    uploaded_by     UUID REFERENCES organization_users(id),
    document_type   VARCHAR(100) NOT NULL DEFAULT 'OTHER',
    file_name       VARCHAR(255),
    file_url        TEXT,
    status          VARCHAR(30)  NOT NULL DEFAULT 'pending',
    reviewer_note   TEXT,
    reviewed_by     UUID REFERENCES users(id),
    reviewed_at     TIMESTAMPTZ,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS service_requests (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    request_number      VARCHAR(30) UNIQUE,
    organization_id     UUID NOT NULL REFERENCES organizations(id),
    department_id       VARCHAR(50) NOT NULL DEFAULT 'loans',
    consultant_id       UUID REFERENCES users(id),
    department_admin_id UUID REFERENCES users(id),
    title               VARCHAR(255) NOT NULL,
    description         TEXT,
    priority            VARCHAR(20)  NOT NULL DEFAULT 'medium',
    status              VARCHAR(50)  NOT NULL DEFAULT 'submitted',
    amount_involved     NUMERIC(18,2),
    currency            VARCHAR(10)  DEFAULT 'INR',
    notes               TEXT,
    closed_at           TIMESTAMPTZ,
    is_active           BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at          TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS organization_proposals (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    service_request_id  UUID REFERENCES service_requests(id),
    organization_id     UUID NOT NULL REFERENCES organizations(id),
    consultant_id       UUID NOT NULL REFERENCES users(id),
    department          VARCHAR(50)  NOT NULL DEFAULT 'loans',
    title               VARCHAR(255) NOT NULL,
    summary             TEXT,
    details             JSONB        DEFAULT '{}',
    fee_amount          NUMERIC(18,2),
    currency            VARCHAR(10)  DEFAULT 'INR',
    status              VARCHAR(50)  NOT NULL DEFAULT 'draft',
    org_feedback        TEXT,
    valid_until         DATE,
    is_active           BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at          TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS organization_meetings (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id     UUID NOT NULL REFERENCES organizations(id),
    consultant_id       UUID REFERENCES users(id),
    service_request_id  UUID REFERENCES service_requests(id),
    title               VARCHAR(255) NOT NULL,
    meeting_type        VARCHAR(50)  DEFAULT 'video',
    scheduled_at        TIMESTAMPTZ,
    duration_minutes    INTEGER      DEFAULT 60,
    meeting_link        TEXT,
    agenda              TEXT,
    status              VARCHAR(30)  NOT NULL DEFAULT 'scheduled',
    notes               TEXT,
    created_at          TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS organization_payments (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    payment_number      VARCHAR(30) UNIQUE,
    organization_id     UUID NOT NULL REFERENCES organizations(id),
    service_request_id  UUID REFERENCES service_requests(id),
    proposal_id         UUID REFERENCES organization_proposals(id),
    amount              NUMERIC(18,2) NOT NULL,
    currency            VARCHAR(10)   DEFAULT 'INR',
    gateway             VARCHAR(30)   DEFAULT 'razorpay',
    gateway_order_id    VARCHAR(255),
    gateway_payment_id  VARCHAR(255),
    status              VARCHAR(30)   NOT NULL DEFAULT 'pending',
    invoice_url         TEXT,
    paid_at             TIMESTAMPTZ,
    created_at          TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ   NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS support_tickets (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    ticket_number   VARCHAR(30) UNIQUE,
    organization_id UUID NOT NULL REFERENCES organizations(id),
    raised_by       UUID REFERENCES organization_users(id),
    subject         VARCHAR(255) NOT NULL,
    description     TEXT,
    category        VARCHAR(50)  DEFAULT 'general',
    priority        VARCHAR(20)  DEFAULT 'medium',
    status          VARCHAR(30)  NOT NULL DEFAULT 'open',
    assigned_to     UUID REFERENCES users(id),
    resolved_at     TIMESTAMPTZ,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_org_docs_org        ON organization_documents(organization_id);
CREATE INDEX IF NOT EXISTS idx_svc_req_org         ON service_requests(organization_id);
CREATE INDEX IF NOT EXISTS idx_svc_req_dept        ON service_requests(department_id);
CREATE INDEX IF NOT EXISTS idx_org_proposals_org   ON organization_proposals(organization_id);
CREATE INDEX IF NOT EXISTS idx_org_meetings_org    ON organization_meetings(organization_id);
CREATE INDEX IF NOT EXISTS idx_org_payments_org    ON organization_payments(organization_id);
CREATE INDEX IF NOT EXISTS idx_support_tickets_org ON support_tickets(organization_id);
