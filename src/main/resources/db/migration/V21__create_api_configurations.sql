-- V21__create_api_configurations.sql
-- Support department-wise API keys and secrets for third-party recommendation APIs

CREATE TABLE api_configurations (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    department      VARCHAR(50) NOT NULL,
    name            VARCHAR(100) NOT NULL,
    api_url         VARCHAR(500) NOT NULL,
    api_key         VARCHAR(500),
    api_secret      VARCHAR(500),
    is_active       BOOLEAN NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at      TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

CREATE INDEX idx_api_config_dept_active ON api_configurations(department, is_active);
