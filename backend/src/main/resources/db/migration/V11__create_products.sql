-- Produto = anúncio de veículo. Sempre vinculado a uma loja física.
-- A geolocalização do anúncio pode ser a da loja (padrão) ou personalizada.
CREATE TABLE products (
    id                    UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id             UUID NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    physical_store_id     UUID NOT NULL REFERENCES physical_stores(id) ON DELETE RESTRICT,
    category_id           UUID NOT NULL REFERENCES categories(id),
    brand_id              INTEGER REFERENCES vehicle_brands(id),
    model_id              INTEGER REFERENCES vehicle_models(id),
    realm                 VARCHAR(20) NOT NULL,                 -- CAR | MOTORCYCLE | TRUCK | NAUTICAL | BUS
    year_model            SMALLINT,
    year_build            SMALLINT,
    mileage_km            INTEGER,
    fuel                  VARCHAR(40),
    transmission          VARCHAR(40),
    attributes            JSONB NOT NULL DEFAULT '{}'::jsonb,    -- validados por attribute_schemas
    status                VARCHAR(20) NOT NULL DEFAULT 'DRAFT',  -- DRAFT | ACTIVE | INACTIVE | ARCHIVED | SOLD
    posted_to_instagram_at TIMESTAMPTZ,
    instagram_media_id    VARCHAR(80),
    created_by_user_id    UUID REFERENCES users(id),
    created_at            TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at            TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    deleted_at            TIMESTAMPTZ
);

CREATE INDEX idx_products_tenant_status ON products(tenant_id, status) WHERE deleted_at IS NULL;
CREATE INDEX idx_products_store ON products(physical_store_id) WHERE deleted_at IS NULL;
CREATE INDEX idx_products_category ON products(category_id);
CREATE INDEX idx_products_brand_model ON products(brand_id, model_id);

-- Localizações: título, descrição, preço e geolocalização.
-- Multi-currency-ready (CHAR(3) currency).
-- A geolocalização pode herdar da loja (location_source = 'STORE') ou ser customizada ('CUSTOM').
CREATE TABLE product_localizations (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    product_id          UUID NOT NULL REFERENCES products(id) ON DELETE CASCADE,
    country_code        CHAR(2) NOT NULL REFERENCES countries(code),
    title               VARCHAR(200) NOT NULL,
    description         TEXT,
    price_cents         BIGINT NOT NULL,
    currency            CHAR(3) NOT NULL DEFAULT 'BRL',
    location            geography(Point, 4326),                   -- null quando location_source = STORE
    location_source     VARCHAR(20) NOT NULL DEFAULT 'STORE',     -- STORE | CUSTOM
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE (product_id, country_code)
);

CREATE INDEX idx_product_loc_product ON product_localizations(product_id);
CREATE INDEX idx_product_loc_location ON product_localizations USING GIST (location);

-- Imagens do produto (chaves no S3).
CREATE TABLE product_images (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    product_id      UUID NOT NULL REFERENCES products(id) ON DELETE CASCADE,
    s3_key          VARCHAR(512) NOT NULL,
    s3_bucket       VARCHAR(120) NOT NULL,
    public_url      VARCHAR(1024) NOT NULL,
    content_type    VARCHAR(60),
    width           INTEGER,
    height          INTEGER,
    sort_order      SMALLINT NOT NULL DEFAULT 0,
    is_cover        BOOLEAN NOT NULL DEFAULT FALSE,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_product_images_product ON product_images(product_id, sort_order);

-- Garante apenas 1 imagem de capa por produto.
CREATE UNIQUE INDEX uk_product_images_cover
    ON product_images(product_id)
    WHERE is_cover = TRUE;
