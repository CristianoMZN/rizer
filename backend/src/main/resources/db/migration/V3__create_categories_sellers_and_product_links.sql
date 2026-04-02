CREATE TABLE IF NOT EXISTS categories (
    id BIGSERIAL PRIMARY KEY,
    slug VARCHAR(120) NOT NULL UNIQUE,
    name VARCHAR(120) NOT NULL,
    description VARCHAR(500),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS subcategories (
    id BIGSERIAL PRIMARY KEY,
    category_id BIGINT NOT NULL REFERENCES categories (id) ON DELETE RESTRICT,
    slug VARCHAR(120) NOT NULL,
    name VARCHAR(120) NOT NULL,
    description VARCHAR(500),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uk_subcategories_category_slug UNIQUE (category_id, slug)
);

CREATE INDEX IF NOT EXISTS idx_subcategories_category_id ON subcategories (category_id);

CREATE TABLE IF NOT EXISTS subsubcategories (
    id BIGSERIAL PRIMARY KEY,
    subcategory_id BIGINT NOT NULL REFERENCES subcategories (id) ON DELETE RESTRICT,
    slug VARCHAR(120) NOT NULL,
    name VARCHAR(120) NOT NULL,
    description VARCHAR(500),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uk_subsubcategories_subcategory_slug UNIQUE (subcategory_id, slug)
);

CREATE INDEX IF NOT EXISTS idx_subsubcategories_subcategory_id ON subsubcategories (subcategory_id);

CREATE TABLE IF NOT EXISTS sellers (
    id BIGSERIAL PRIMARY KEY,
    user_id UUID REFERENCES users (id) ON DELETE SET NULL,
    display_name VARCHAR(255) NOT NULL,
    legal_name VARCHAR(255),
    tax_id VARCHAR(30),
    phone VARCHAR(30),
    email VARCHAR(255),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uk_sellers_user_id UNIQUE (user_id),
    CONSTRAINT uk_sellers_tax_id UNIQUE (tax_id)
);

ALTER TABLE products
    ADD COLUMN IF NOT EXISTS seller_id BIGINT,
    ADD COLUMN IF NOT EXISTS subsubcategory_id BIGINT;

CREATE INDEX IF NOT EXISTS idx_products_seller_id ON products (seller_id);
CREATE INDEX IF NOT EXISTS idx_products_subsubcategory_id ON products (subsubcategory_id);

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'fk_products_seller_id'
    ) THEN
        ALTER TABLE products
            ADD CONSTRAINT fk_products_seller_id
            FOREIGN KEY (seller_id)
            REFERENCES sellers (id)
            ON DELETE SET NULL;
    END IF;
END $$;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'fk_products_subsubcategory_id'
    ) THEN
        ALTER TABLE products
            ADD CONSTRAINT fk_products_subsubcategory_id
            FOREIGN KEY (subsubcategory_id)
            REFERENCES subsubcategories (id)
            ON DELETE SET NULL;
    END IF;
END $$;
