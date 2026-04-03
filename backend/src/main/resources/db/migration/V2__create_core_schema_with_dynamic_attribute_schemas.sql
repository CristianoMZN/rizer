CREATE TABLE IF NOT EXISTS users (
    id UUID PRIMARY KEY,
    email VARCHAR(255) NOT NULL UNIQUE,
    name VARCHAR(255) NOT NULL,
    avatar_url VARCHAR(512),
    provider VARCHAR(50) NOT NULL,
    provider_id VARCHAR(255) NOT NULL,
    system_role VARCHAR(30) NOT NULL DEFAULT 'USER',
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_users_provider_provider_id ON users (provider, provider_id);

CREATE TABLE IF NOT EXISTS tenants (
    id UUID PRIMARY KEY,
    seller_id BIGINT NOT NULL UNIQUE,
    slug VARCHAR(120) NOT NULL UNIQUE,
    name VARCHAR(255) NOT NULL,
    is_public BOOLEAN NOT NULL DEFAULT FALSE,
    status VARCHAR(30) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS seller_users (
    id BIGSERIAL PRIMARY KEY,
    seller_id BIGINT NOT NULL,
    user_id UUID NOT NULL,
    role VARCHAR(30) NOT NULL,
    is_owner BOOLEAN NOT NULL DEFAULT FALSE,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uk_seller_users_seller_user UNIQUE (seller_id, user_id),
    CONSTRAINT fk_seller_users_user FOREIGN KEY (user_id) REFERENCES users(id)
);

CREATE INDEX IF NOT EXISTS idx_seller_users_seller_id ON seller_users (seller_id);
CREATE INDEX IF NOT EXISTS idx_seller_users_user_id ON seller_users (user_id);

CREATE TABLE IF NOT EXISTS subsubcategories (
    id BIGSERIAL PRIMARY KEY,
    slug VARCHAR(120) NOT NULL UNIQUE
);

CREATE TABLE IF NOT EXISTS products (
    id BIGSERIAL PRIMARY KEY,
    uuid UUID NOT NULL UNIQUE,
    merchant_id UUID NOT NULL,
    tenant_id UUID,
    seller_id BIGINT,
    subsubcategory_id BIGINT,
    created_by_user_id UUID,
    status VARCHAR(30) NOT NULL,
    realm VARCHAR(40) NOT NULL,
    category_path LTREE NOT NULL,
    attributes JSONB NOT NULL DEFAULT '{}'::jsonb,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_products_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id),
    CONSTRAINT fk_products_subsubcategory FOREIGN KEY (subsubcategory_id) REFERENCES subsubcategories(id),
    CONSTRAINT fk_products_created_by_user FOREIGN KEY (created_by_user_id) REFERENCES users(id)
);

CREATE INDEX IF NOT EXISTS idx_products_uuid ON products (uuid);
CREATE INDEX IF NOT EXISTS idx_products_realm ON products (realm);
CREATE INDEX IF NOT EXISTS idx_products_merchant_id ON products (merchant_id);
CREATE INDEX IF NOT EXISTS idx_products_category_path ON products USING GIST (category_path);
CREATE INDEX IF NOT EXISTS idx_products_attributes_gin ON products USING GIN (attributes);

CREATE TABLE IF NOT EXISTS product_localizations (
    id BIGSERIAL PRIMARY KEY,
    product_id BIGINT NOT NULL,
    country_code VARCHAR(2) NOT NULL,
    title VARCHAR(200) NOT NULL,
    description VARCHAR(2000),
    price NUMERIC(14,2) NOT NULL,
    currency VARCHAR(3) NOT NULL,
    unit_system VARCHAR(20) NOT NULL,
    location geometry(Point,4326) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_product_localizations_product FOREIGN KEY (product_id) REFERENCES products(id) ON DELETE CASCADE,
    CONSTRAINT uk_product_country UNIQUE (product_id, country_code)
);

CREATE INDEX IF NOT EXISTS idx_product_localizations_country ON product_localizations (country_code);
CREATE INDEX IF NOT EXISTS idx_product_localizations_price ON product_localizations (price);
CREATE INDEX IF NOT EXISTS idx_product_localizations_currency ON product_localizations (currency);
CREATE INDEX IF NOT EXISTS idx_product_localizations_location_gist ON product_localizations USING GIST (location);

-- Nova tabela de schemas dinâmicos em JSONB por contexto (entidade + pais + categoria).
CREATE TABLE IF NOT EXISTS attribute_schemas (
    id BIGSERIAL PRIMARY KEY,
    entity_type VARCHAR(50) NOT NULL,
    country_code VARCHAR(2) NOT NULL,
    category_path LTREE NOT NULL,
    version INTEGER NOT NULL DEFAULT 1,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    schema_definition JSONB NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uk_attribute_schemas_context_version UNIQUE (entity_type, country_code, category_path, version)
);

CREATE INDEX IF NOT EXISTS idx_attribute_schemas_active_context
    ON attribute_schemas (entity_type, country_code, category_path, is_active, version DESC);

CREATE INDEX IF NOT EXISTS idx_attribute_schemas_definition_gin
    ON attribute_schemas USING GIN (schema_definition);

INSERT INTO subsubcategories (slug)
VALUES ('suv')
ON CONFLICT (slug) DO NOTHING;

INSERT INTO attribute_schemas (entity_type, country_code, category_path, version, is_active, schema_definition)
VALUES (
    'PRODUCT_ATTRIBUTES',
    '*',
    'veiculos.passeio.suvs'::ltree,
    1,
    TRUE,
    '{"type":"object","additionalProperties":true}'::jsonb
)
ON CONFLICT (entity_type, country_code, category_path, version) DO NOTHING;

INSERT INTO attribute_schemas (entity_type, country_code, category_path, version, is_active, schema_definition)
VALUES (
    'PRODUCT_ATTRIBUTES',
    'BR',
    'veiculos.passeio.suvs'::ltree,
    1,
    TRUE,
    '{
      "type": "object",
      "additionalProperties": false,
      "required": ["identificacao", "motor"],
      "properties": {
        "identificacao": {
          "type": "object",
          "additionalProperties": false,
          "required": ["marca", "modelo", "preco", "combustivel"],
          "properties": {
            "marca": {"type": "string", "minLength": 1, "maxLength": 80},
            "modelo": {"type": "string", "minLength": 1, "maxLength": 120},
            "preco": {"type": "number", "minimum": 0},
            "combustivel": {"type": "string", "enum": ["gasolina", "etanol", "flex", "diesel", "hibrido", "eletrico"]},
            "ano_modelo": {"type": "integer", "minimum": 1950, "maximum": 2100}
          }
        },
        "motor": {
          "type": "object",
          "additionalProperties": false,
          "required": ["potencia_cv", "torque_nm"],
          "properties": {
            "potencia_cv": {"type": "number", "minimum": 1},
            "torque_nm": {"type": "number", "minimum": 1},
            "cambio": {"type": "string", "maxLength": 80}
          }
        }
      }
    }'::jsonb
)
ON CONFLICT (entity_type, country_code, category_path, version) DO NOTHING;