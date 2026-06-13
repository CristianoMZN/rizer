-- Tokens de integração (Instagram / META Business / Google) por tenant.
-- Tokens sensíveis são encriptados pelo EncryptionService antes de persistir.
CREATE TYPE integration_provider AS ENUM (
    'INSTAGRAM',
    'META_BUSINESS',
    'GOOGLE_MERCHANT'
);

CREATE TYPE integration_status AS ENUM (
    'CONNECTED',
    'REFRESHING',
    'EXPIRED',
    'REVOKED',
    'ERROR'
);

CREATE TABLE tenant_integrations (
    id                        UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id                 UUID NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    provider                  integration_provider NOT NULL,
    external_account_id       VARCHAR(120),                    -- IG user id, Meta Business id, GMC id
    external_account_name     VARCHAR(255),
    access_token_encrypted    TEXT,                            -- AES-GCM base64
    refresh_token_encrypted   TEXT,
    token_expires_at          TIMESTAMPTZ,
    scopes                    TEXT,                            -- CSV com scopes OAuth concedidos
    status                    integration_status NOT NULL DEFAULT 'CONNECTED',
    last_sync_at              TIMESTAMPTZ,
    last_error_at             TIMESTAMPTZ,
    last_error_message        TEXT,
    raw_metadata              JSONB NOT NULL DEFAULT '{}'::jsonb,
    created_at                TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at                TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE (tenant_id, provider)
);

CREATE INDEX idx_tenant_integrations_tenant ON tenant_integrations(tenant_id);
CREATE INDEX idx_tenant_integrations_provider ON tenant_integrations(provider, status);
