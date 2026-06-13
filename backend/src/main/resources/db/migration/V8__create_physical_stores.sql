-- Filiais (lojas físicas) vinculadas a um tenant.
-- Cada tenant pode ter 1..N (limite enforçado no service de acordo com o plano).
-- Geolocalização vem do endereço para permitir filtros de proximidade.
CREATE TABLE physical_stores (
    id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id         UUID NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    address_id        UUID REFERENCES addresses(id),
    name              VARCHAR(255) NOT NULL,
    slug              VARCHAR(120) NOT NULL,
    phone             VARCHAR(32),
    whatsapp          VARCHAR(32),
    email             VARCHAR(255),
    opening_hours     JSONB NOT NULL DEFAULT '{}'::jsonb,  -- { monday: { open: "08:00", close: "18:00" }, ... }
    is_main           BOOLEAN NOT NULL DEFAULT FALSE,
    is_active         BOOLEAN NOT NULL DEFAULT TRUE,
    location          geography(Point, 4326),
    created_by_user_id UUID REFERENCES users(id),
    created_at        TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at        TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    deleted_at        TIMESTAMPTZ,
    UNIQUE (tenant_id, slug)
);

CREATE INDEX idx_physical_stores_tenant ON physical_stores(tenant_id) WHERE deleted_at IS NULL;
CREATE INDEX idx_physical_stores_active ON physical_stores(tenant_id, is_active) WHERE deleted_at IS NULL;
CREATE INDEX idx_physical_stores_location ON physical_stores USING GIST (location);

-- Garante apenas UMA loja principal por tenant.
CREATE UNIQUE INDEX uk_physical_stores_main
    ON physical_stores(tenant_id)
    WHERE is_main = TRUE AND deleted_at IS NULL;
