CREATE TABLE IF NOT EXISTS users (
    id UUID PRIMARY KEY,
    email VARCHAR(255) NOT NULL UNIQUE,
    name VARCHAR(255) NOT NULL,
    avatar_url VARCHAR(512),
    provider VARCHAR(50) NOT NULL,
    provider_id VARCHAR(255) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS products (
    id BIGSERIAL PRIMARY KEY,
    uuid UUID NOT NULL UNIQUE,
    merchant_id UUID NOT NULL,
    realm VARCHAR(40) NOT NULL,
    category_path LTREE NOT NULL,
    attributes JSONB NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_products_uuid ON products (uuid);
CREATE INDEX IF NOT EXISTS idx_products_realm ON products (realm);
CREATE INDEX IF NOT EXISTS idx_products_merchant_id ON products (merchant_id);

CREATE TABLE IF NOT EXISTS product_localizations (
    id BIGSERIAL PRIMARY KEY,
    product_id BIGINT NOT NULL REFERENCES products (id) ON DELETE CASCADE,
    country_code VARCHAR(2) NOT NULL,
    title VARCHAR(200) NOT NULL,
    description VARCHAR(2000),
    price NUMERIC(14,2) NOT NULL,
    currency VARCHAR(3) NOT NULL,
    unit_system VARCHAR(20) NOT NULL,
    location geometry(Point, 4326) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uk_product_country UNIQUE (product_id, country_code)
);

CREATE INDEX IF NOT EXISTS idx_product_localizations_country ON product_localizations (country_code);
CREATE INDEX IF NOT EXISTS idx_product_localizations_price ON product_localizations (price);
CREATE INDEX IF NOT EXISTS idx_product_localizations_currency ON product_localizations (currency);
CREATE INDEX IF NOT EXISTS idx_product_localizations_location_gist ON product_localizations USING GIST (location);
