-- Tenant = a empresa/rede. Tem 1..N physical_stores (criado em V8).
-- Cada tenant vive dentro de um country (region context).
CREATE TABLE tenants (
    id                          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    country_code                VARCHAR(2) NOT NULL REFERENCES countries(code),
    slug                        VARCHAR(80) NOT NULL,                       -- usado no subdomínio slug.motorise.com.br
    cnpj                        VARCHAR(20),
    legal_name                  VARCHAR(255),                               -- razão social
    trade_name                  VARCHAR(255) NOT NULL,                      -- nome fantasia (exibido no header)
    description                 TEXT,
    logo_url                    VARCHAR(512),
    banner_url                  VARCHAR(512),
    phone                       VARCHAR(32),
    whatsapp                    VARCHAR(32),
    email                       VARCHAR(255),
    website                     VARCHAR(255),
    theme                       JSONB NOT NULL DEFAULT '{}'::jsonb,        -- { primary, secondary, accent, dark, darkPage }
    custom_domain               VARCHAR(255),                               -- ex.: minha-concessionaria.com.br
    custom_domain_status        VARCHAR(20) NOT NULL DEFAULT 'NONE',        -- NONE | PENDING | VERIFIED | FAILED
    custom_domain_last_check_at TIMESTAMPTZ,
    custom_domain_error         TEXT,
    status                      VARCHAR(20) NOT NULL DEFAULT 'pending',     -- pending | active | paused | suspended | canceled
    is_public                   BOOLEAN NOT NULL DEFAULT FALSE,            -- aparece em /parceiros (PRO/Platinum nascem true)
    is_partner_page_enabled     BOOLEAN NOT NULL DEFAULT FALSE,
    had_trial                   BOOLEAN NOT NULL DEFAULT FALSE,            -- trial só uma vez por tenant
    attributes                  JSONB NOT NULL DEFAULT '{}'::jsonb,
    created_by_user_id          UUID REFERENCES users(id),
    created_at                  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at                  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    deleted_at                  TIMESTAMPTZ,
    UNIQUE (country_code, slug)
);

CREATE UNIQUE INDEX uk_tenants_custom_domain ON tenants(custom_domain) WHERE custom_domain IS NOT NULL;
CREATE INDEX idx_tenants_status ON tenants(status) WHERE deleted_at IS NULL;
CREATE INDEX idx_tenants_public ON tenants(is_public, is_partner_page_enabled) WHERE deleted_at IS NULL;

-- Associação usuário ↔ tenant com papel DENTRO do tenant.
-- O papel NA PLATAFORMA fica em users.system_role.
CREATE TABLE tenant_users (
    id                    UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id             UUID NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    user_id               UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    role                  VARCHAR(20) NOT NULL DEFAULT 'SELLER',           -- OWNER | MANAGER | SELLER
    physical_store_ids    UUID[] NOT NULL DEFAULT '{}'::uuid[],            -- restringe SELLER a filiais específicas (vazio = todas)
    is_active             BOOLEAN NOT NULL DEFAULT TRUE,
    invited_by_user_id    UUID REFERENCES users(id),
    invited_at            TIMESTAMPTZ,
    accepted_at           TIMESTAMPTZ,
    expire_at             TIMESTAMPTZ,                                     -- expiração do vínculo (opcional)
    created_at            TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at            TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE (tenant_id, user_id)
);

CREATE INDEX idx_tenant_users_user ON tenant_users(user_id) WHERE is_active = TRUE;
CREATE INDEX idx_tenant_users_tenant ON tenant_users(tenant_id) WHERE is_active = TRUE;
