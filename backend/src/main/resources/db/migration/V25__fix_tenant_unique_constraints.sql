-- Remove UNIQUE constraint total e substitui por partial unique index
-- para permitir slugs reutilizáveis após soft-delete.
DROP INDEX IF EXISTS uk_tenants_custom_domain;

ALTER TABLE tenants DROP CONSTRAINT IF EXISTS tenants_country_code_slug_key;

CREATE UNIQUE INDEX IF NOT EXISTS idx_tenants_slug_unique ON tenants(country_code, slug) WHERE deleted_at IS NULL;

CREATE UNIQUE INDEX IF NOT EXISTS idx_tenants_custom_domain ON tenants(custom_domain) WHERE custom_domain IS NOT NULL AND deleted_at IS NULL;
