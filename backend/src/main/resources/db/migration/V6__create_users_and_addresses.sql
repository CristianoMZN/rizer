-- Usuários globais da plataforma.
-- system_role define o papel NA PLATAFORMA. O papel DENTRO de um tenant
-- fica em tenant_users.role (criado em V7).
CREATE TABLE users (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email           VARCHAR(255) NOT NULL UNIQUE,
    name            VARCHAR(255) NOT NULL,
    phone           VARCHAR(32),
    password_hash   VARCHAR(255),                                  -- bcrypt; NULL para login só OAuth
    avatar_url      VARCHAR(512),
    provider        VARCHAR(50) NOT NULL DEFAULT 'local',         -- local | google
    provider_id     VARCHAR(255),                                 -- id do usuário no provedor OAuth
    system_role     VARCHAR(30) NOT NULL DEFAULT 'user',          -- user | agency_employee | agency_admin | agency_owner | sys_employee | sys_manager | sys_admin
    attributes      JSONB NOT NULL DEFAULT '{}'::jsonb,
    is_active       BOOLEAN NOT NULL DEFAULT TRUE,
    last_login_at   TIMESTAMPTZ,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    deleted_at      TIMESTAMPTZ
);

-- Lookup por provedor OAuth precisa ser único por par (provider, provider_id) quando provider_id existe.
CREATE UNIQUE INDEX uk_users_provider ON users(provider, provider_id) WHERE provider_id IS NOT NULL;

CREATE INDEX idx_users_system_role ON users(system_role);
CREATE INDEX idx_users_active ON users(is_active) WHERE deleted_at IS NULL;

-- Endereços vinculados a usuários (ou, em fase futura, a tenants diretamente).
CREATE TABLE addresses (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    country_code    VARCHAR(2) NOT NULL REFERENCES countries(code),
    user_id         UUID REFERENCES users(id) ON DELETE CASCADE,
    label           VARCHAR(80),                                   -- ex.: "casa", "trabalho"
    zip_code        VARCHAR(16),
    street          VARCHAR(255),
    number          VARCHAR(32),
    complement      VARCHAR(120),
    neighborhood    VARCHAR(120),
    city            VARCHAR(120),
    state           VARCHAR(80),
    country         VARCHAR(80),                                   -- nome do país (desnormalizado para exibição)
    location        geography(Point, 4326),                        -- PostGIS
    is_primary      BOOLEAN NOT NULL DEFAULT FALSE,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    deleted_at      TIMESTAMPTZ
);

CREATE INDEX idx_addresses_user ON addresses(user_id) WHERE deleted_at IS NULL;
CREATE INDEX idx_addresses_location ON addresses USING GIST (location);
