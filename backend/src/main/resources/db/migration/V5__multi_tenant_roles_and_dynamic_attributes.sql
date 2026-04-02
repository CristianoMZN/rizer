CREATE EXTENSION IF NOT EXISTS pgcrypto;

ALTER TABLE users
    ADD COLUMN IF NOT EXISTS system_role VARCHAR(30) NOT NULL DEFAULT 'USER';

CREATE TABLE IF NOT EXISTS tenants (
    id UUID PRIMARY KEY,
    seller_id BIGINT NOT NULL UNIQUE REFERENCES sellers (id) ON DELETE CASCADE,
    slug VARCHAR(120) NOT NULL UNIQUE,
    name VARCHAR(255) NOT NULL,
    domain VARCHAR(255),
    subdomain VARCHAR(120),
    theme VARCHAR(60),
    is_public BOOLEAN NOT NULL DEFAULT TRUE,
    status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS seller_users (
    id BIGSERIAL PRIMARY KEY,
    seller_id BIGINT NOT NULL REFERENCES sellers (id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    role VARCHAR(30) NOT NULL,
    is_owner BOOLEAN NOT NULL DEFAULT FALSE,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uk_seller_users_seller_user UNIQUE (seller_id, user_id)
);

CREATE INDEX IF NOT EXISTS idx_seller_users_user_id ON seller_users (user_id);
CREATE INDEX IF NOT EXISTS idx_seller_users_seller_id ON seller_users (seller_id);
CREATE INDEX IF NOT EXISTS idx_seller_users_role ON seller_users (role);

CREATE TABLE IF NOT EXISTS country_configurations (
    country_code VARCHAR(2) PRIMARY KEY,
    currency VARCHAR(3) NOT NULL,
    locale VARCHAR(20) NOT NULL,
    fiscal_rules JSONB NOT NULL DEFAULT '{}'::jsonb,
    payment_methods JSONB NOT NULL DEFAULT '[]'::jsonb,
    integrations JSONB NOT NULL DEFAULT '{}'::jsonb,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS tenant_configurations (
    id BIGSERIAL PRIMARY KEY,
    tenant_id UUID NOT NULL REFERENCES tenants (id) ON DELETE CASCADE,
    country_code VARCHAR(2) NOT NULL REFERENCES country_configurations (country_code) ON DELETE RESTRICT,
    config JSONB NOT NULL DEFAULT '{}'::jsonb,
    integrations JSONB NOT NULL DEFAULT '{}'::jsonb,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uk_tenant_configurations_tenant_country UNIQUE (tenant_id, country_code)
);

CREATE INDEX IF NOT EXISTS idx_tenant_configurations_tenant_id ON tenant_configurations (tenant_id);
CREATE INDEX IF NOT EXISTS idx_tenant_configurations_country_code ON tenant_configurations (country_code);

CREATE TABLE IF NOT EXISTS attribute_groups (
    id BIGSERIAL PRIMARY KEY,
    code VARCHAR(120) NOT NULL UNIQUE,
    name VARCHAR(120) NOT NULL,
    description VARCHAR(500),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS attribute_definitions (
    id BIGSERIAL PRIMARY KEY,
    group_id BIGINT NOT NULL REFERENCES attribute_groups (id) ON DELETE CASCADE,
    code VARCHAR(120) NOT NULL,
    name VARCHAR(120) NOT NULL,
    data_type VARCHAR(30) NOT NULL,
    required BOOLEAN NOT NULL DEFAULT FALSE,
    validation_rules JSONB NOT NULL DEFAULT '{}'::jsonb,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    sort_order INT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uk_attribute_definitions_group_code UNIQUE (group_id, code)
);

CREATE INDEX IF NOT EXISTS idx_attribute_definitions_group_id ON attribute_definitions (group_id);
CREATE INDEX IF NOT EXISTS idx_attribute_definitions_data_type ON attribute_definitions (data_type);

CREATE TABLE IF NOT EXISTS category_attribute_groups (
    id BIGSERIAL PRIMARY KEY,
    subsubcategory_id BIGINT NOT NULL REFERENCES subsubcategories (id) ON DELETE CASCADE,
    group_id BIGINT NOT NULL REFERENCES attribute_groups (id) ON DELETE CASCADE,
    required BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uk_category_attribute_groups_subsubcategory_group UNIQUE (subsubcategory_id, group_id)
);

CREATE INDEX IF NOT EXISTS idx_category_attribute_groups_subsubcategory_id ON category_attribute_groups (subsubcategory_id);
CREATE INDEX IF NOT EXISTS idx_category_attribute_groups_group_id ON category_attribute_groups (group_id);

CREATE TABLE IF NOT EXISTS product_attribute_values (
    id BIGSERIAL PRIMARY KEY,
    product_id BIGINT NOT NULL REFERENCES products (id) ON DELETE CASCADE,
    attribute_definition_id BIGINT NOT NULL REFERENCES attribute_definitions (id) ON DELETE RESTRICT,
    value_text TEXT,
    value_number NUMERIC(20, 6),
    value_boolean BOOLEAN,
    value_date DATE,
    value_json JSONB,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uk_product_attribute_values_product_attr UNIQUE (product_id, attribute_definition_id),
    CONSTRAINT chk_product_attribute_values_any_value
        CHECK (
            value_text IS NOT NULL
            OR value_number IS NOT NULL
            OR value_boolean IS NOT NULL
            OR value_date IS NOT NULL
            OR value_json IS NOT NULL
        )
);

CREATE INDEX IF NOT EXISTS idx_product_attribute_values_product_id ON product_attribute_values (product_id);
CREATE INDEX IF NOT EXISTS idx_product_attribute_values_attribute_definition_id ON product_attribute_values (attribute_definition_id);

ALTER TABLE products
    ADD COLUMN IF NOT EXISTS tenant_id UUID,
    ADD COLUMN IF NOT EXISTS created_by_user_id UUID,
    ADD COLUMN IF NOT EXISTS status VARCHAR(30) NOT NULL DEFAULT 'DRAFT';

CREATE INDEX IF NOT EXISTS idx_products_tenant_id ON products (tenant_id);
CREATE INDEX IF NOT EXISTS idx_products_created_by_user_id ON products (created_by_user_id);
CREATE INDEX IF NOT EXISTS idx_products_status ON products (status);

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'fk_products_tenant_id'
    ) THEN
        ALTER TABLE products
            ADD CONSTRAINT fk_products_tenant_id
            FOREIGN KEY (tenant_id)
            REFERENCES tenants (id)
            ON DELETE SET NULL;
    END IF;
END $$;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'fk_products_created_by_user_id'
    ) THEN
        ALTER TABLE products
            ADD CONSTRAINT fk_products_created_by_user_id
            FOREIGN KEY (created_by_user_id)
            REFERENCES users (id)
            ON DELETE SET NULL;
    END IF;
END $$;

UPDATE users
SET system_role = 'SELLER'
WHERE email LIKE 'vendedor%@riser.local';

INSERT INTO tenants (id, seller_id, slug, name, subdomain, theme, created_at, updated_at)
SELECT
    gen_random_uuid(),
    s.id,
    LOWER(REGEXP_REPLACE(s.display_name, '[^a-zA-Z0-9]+', '-', 'g')),
    s.display_name,
    LOWER(REGEXP_REPLACE(s.display_name, '[^a-zA-Z0-9]+', '-', 'g')),
    'default',
    NOW(),
    NOW()
FROM sellers s
ON CONFLICT (seller_id) DO NOTHING;

UPDATE products p
SET
    tenant_id = t.id,
    created_by_user_id = COALESCE(p.created_by_user_id, p.merchant_id),
    status = 'ACTIVE'
FROM tenants t
WHERE t.seller_id = p.seller_id
  AND p.tenant_id IS NULL;

INSERT INTO seller_users (seller_id, user_id, role, is_owner, active, created_at, updated_at)
SELECT s.id, u.id, 'SELLER', TRUE, TRUE, NOW(), NOW()
FROM sellers s
JOIN users u ON u.id = s.user_id
ON CONFLICT (seller_id, user_id) DO NOTHING;

INSERT INTO country_configurations (country_code, currency, locale, fiscal_rules, payment_methods, integrations)
VALUES
    ('BR', 'BRL', 'pt-BR', '{"taxModel":"simples"}'::jsonb, '["pix","credit_card","boleto"]'::jsonb, '{"fiscalProvider":"nfe-default"}'::jsonb),
    ('US', 'USD', 'en-US', '{"taxModel":"sales_tax"}'::jsonb, '["credit_card","ach"]'::jsonb, '{"fiscalProvider":"us-default"}'::jsonb)
ON CONFLICT (country_code) DO NOTHING;

INSERT INTO tenant_configurations (tenant_id, country_code, config, integrations)
SELECT t.id, 'BR', '{"storefront":{"showRatings":true,"showStock":true}}'::jsonb, '{}'::jsonb
FROM tenants t
ON CONFLICT (tenant_id, country_code) DO NOTHING;

INSERT INTO attribute_groups (code, name, description)
VALUES
    ('vehicle_basics', 'Vehicle basics', 'Atributos base para anuncios de veiculos'),
    ('real_estate_basics', 'Real estate basics', 'Atributos base para anuncios de imoveis')
ON CONFLICT (code) DO NOTHING;

INSERT INTO attribute_definitions (group_id, code, name, data_type, required, validation_rules, sort_order)
SELECT g.id, d.code, d.name, d.data_type, d.required, d.validation_rules::jsonb, d.sort_order
FROM attribute_groups g
JOIN (
    VALUES
        ('vehicle_basics', 'marca', 'Marca', 'STRING', TRUE, '{"maxLength":80}', 10),
        ('vehicle_basics', 'modelo', 'Modelo', 'STRING', TRUE, '{"maxLength":80}', 20),
        ('vehicle_basics', 'quilometragem', 'Quilometragem', 'NUMBER', TRUE, '{"min":0}', 30),
        ('vehicle_basics', 'cor', 'Cor', 'STRING', FALSE, '{"maxLength":40}', 40),
        ('real_estate_basics', 'area_m2', 'Area util em m2', 'NUMBER', TRUE, '{"min":1}', 10),
        ('real_estate_basics', 'quartos', 'Numero de quartos', 'NUMBER', TRUE, '{"min":0}', 20),
        ('real_estate_basics', 'banheiros', 'Numero de banheiros', 'NUMBER', TRUE, '{"min":0}', 30),
        ('real_estate_basics', 'vagas', 'Numero de vagas', 'NUMBER', FALSE, '{"min":0}', 40)
) AS d(group_code, code, name, data_type, required, validation_rules, sort_order)
    ON g.code = d.group_code
ON CONFLICT (group_id, code) DO NOTHING;

INSERT INTO category_attribute_groups (subsubcategory_id, group_id, required)
SELECT ssc.id, ag.id, TRUE
FROM subsubcategories ssc
JOIN attribute_groups ag ON ag.code = 'vehicle_basics'
WHERE ssc.slug IN ('suv', 'sedan', 'street')
ON CONFLICT (subsubcategory_id, group_id) DO NOTHING;

INSERT INTO category_attribute_groups (subsubcategory_id, group_id, required)
SELECT ssc.id, ag.id, TRUE
FROM subsubcategories ssc
JOIN attribute_groups ag ON ag.code = 'real_estate_basics'
WHERE ssc.slug IN ('apartamento')
ON CONFLICT (subsubcategory_id, group_id) DO NOTHING;
