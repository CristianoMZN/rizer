-- FASE 10 — Área restrita do logista (Minha Empresa + Meus Anúncios + Vendedores).
--
-- Mudanças:
--   1. tenants: dados do sócio proprietário, telefone administrativo, endereço completo (com PostGIS).
--   2. physical_stores: CNPJ/razão social da filial, telefone administrativo, banner, flag is_branch.
--   3. products: geolocalização no próprio produto (preparado para anúncios sem loja no futuro),
--      e seller_user_id (vendedor responsável, whatsapp exibido no anúncio público).
--   4. tenant_gallery_images + physical_store_gallery_images: galerias (fachada, interior, pátio, equipe).

-- ─── 1. tenants ────────────────────────────────────────────────────────────
ALTER TABLE tenants
    ADD COLUMN IF NOT EXISTS partner_owner_name        VARCHAR(255),
    ADD COLUMN IF NOT EXISTS partner_owner_cpf         VARCHAR(14),
    ADD COLUMN IF NOT EXISTS admin_phone               VARCHAR(32),
    ADD COLUMN IF NOT EXISTS address_zip_code          VARCHAR(16),
    ADD COLUMN IF NOT EXISTS address_street            VARCHAR(255),
    ADD COLUMN IF NOT EXISTS address_number            VARCHAR(32),
    ADD COLUMN IF NOT EXISTS address_complement        VARCHAR(120),
    ADD COLUMN IF NOT EXISTS address_neighborhood      VARCHAR(120),
    ADD COLUMN IF NOT EXISTS address_city              VARCHAR(120),
    ADD COLUMN IF NOT EXISTS address_state             VARCHAR(80),
    ADD COLUMN IF NOT EXISTS address_location          geography(Point, 4326);

CREATE INDEX IF NOT EXISTS idx_tenants_address_location
    ON tenants USING GIST (address_location);

-- ─── 2. physical_stores ───────────────────────────────────────────────────
ALTER TABLE physical_stores
    ADD COLUMN IF NOT EXISTS cnpj                      VARCHAR(20),
    ADD COLUMN IF NOT EXISTS legal_name                VARCHAR(255),
    ADD COLUMN IF NOT EXISTS admin_phone               VARCHAR(32),
    ADD COLUMN IF NOT EXISTS banner_url                VARCHAR(512),
    ADD COLUMN IF NOT EXISTS is_branch                 BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS address_zip_code          VARCHAR(16),
    ADD COLUMN IF NOT EXISTS address_street            VARCHAR(255),
    ADD COLUMN IF NOT EXISTS address_number            VARCHAR(32),
    ADD COLUMN IF NOT EXISTS address_complement        VARCHAR(120),
    ADD COLUMN IF NOT EXISTS address_neighborhood      VARCHAR(120),
    ADD COLUMN IF NOT EXISTS address_city              VARCHAR(120),
    ADD COLUMN IF NOT EXISTS address_state             VARCHAR(80);

-- ─── 3. products ──────────────────────────────────────────────────────────
ALTER TABLE products
    ADD COLUMN IF NOT EXISTS latitude                  DOUBLE PRECISION,
    ADD COLUMN IF NOT EXISTS longitude                 DOUBLE PRECISION,
    ADD COLUMN IF NOT EXISTS location                  geography(Point, 4326),
    ADD COLUMN IF NOT EXISTS location_source           VARCHAR(20) NOT NULL DEFAULT 'STORE',
    ADD COLUMN IF NOT EXISTS seller_user_id            UUID REFERENCES users(id) ON DELETE SET NULL;

CREATE INDEX IF NOT EXISTS idx_products_location
    ON products USING GIST (location);
CREATE INDEX IF NOT EXISTS idx_products_seller
    ON products (seller_user_id) WHERE deleted_at IS NULL;

-- Backfill: herda lat/lng/location da physical_store de cada produto já criado.
UPDATE products p
   SET latitude = ST_Y(s.location::geometry),
       longitude = ST_X(s.location::geometry),
       location = s.location,
       location_source = 'STORE'
  FROM physical_stores s
 WHERE s.id = p.physical_store_id
   AND p.location IS NULL
   AND s.location IS NOT NULL;

-- ─── 4. tenant_gallery_images ─────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS tenant_gallery_images (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id   UUID NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    s3_key      VARCHAR(512) NOT NULL,
    s3_bucket   VARCHAR(120) NOT NULL,
    public_url  VARCHAR(1024) NOT NULL,
    caption     VARCHAR(255),
    sort_order  SMALLINT NOT NULL DEFAULT 0,
    is_cover    BOOLEAN NOT NULL DEFAULT FALSE,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_tenant_gallery_tenant
    ON tenant_gallery_images (tenant_id, sort_order);

CREATE UNIQUE INDEX IF NOT EXISTS uk_tenant_gallery_cover
    ON tenant_gallery_images (tenant_id) WHERE is_cover = TRUE;

-- ─── 5. physical_store_gallery_images ─────────────────────────────────────
CREATE TABLE IF NOT EXISTS physical_store_gallery_images (
    id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    physical_store_id UUID NOT NULL REFERENCES physical_stores(id) ON DELETE CASCADE,
    s3_key            VARCHAR(512) NOT NULL,
    s3_bucket         VARCHAR(120) NOT NULL,
    public_url        VARCHAR(1024) NOT NULL,
    caption           VARCHAR(255),
    sort_order        SMALLINT NOT NULL DEFAULT 0,
    is_cover          BOOLEAN NOT NULL DEFAULT FALSE,
    created_at        TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_store_gallery_store
    ON physical_store_gallery_images (physical_store_id, sort_order);

CREATE UNIQUE INDEX IF NOT EXISTS uk_store_gallery_cover
    ON physical_store_gallery_images (physical_store_id) WHERE is_cover = TRUE;
