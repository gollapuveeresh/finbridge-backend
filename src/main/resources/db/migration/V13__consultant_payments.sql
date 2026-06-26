-- V13__consultant_payments.sql
CREATE TABLE IF NOT EXISTS consultant_payments (
    id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    consultant_id     UUID NOT NULL,
    consultation_id   UUID NOT NULL,
    client_name       VARCHAR(255) NOT NULL,
    department        VARCHAR(50) NOT NULL,
    fee_amount        NUMERIC(15,2) NOT NULL,
    commission_amount NUMERIC(15,2) NOT NULL,
    status            VARCHAR(30) NOT NULL DEFAULT 'pending', -- pending | paid
    processed_at      TIMESTAMPTZ,
    created_at        TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at        TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_consultant_payments_consultant ON consultant_payments(consultant_id);
