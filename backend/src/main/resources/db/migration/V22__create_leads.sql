-- Captura de leads (interesse em veículos).
-- V18 do plano macro.
CREATE TABLE leads (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id       UUID REFERENCES tenants(id),
    product_id      UUID REFERENCES products(id),
    physical_store_id UUID REFERENCES physical_stores(id),
    buyer_name      VARCHAR(120) NOT NULL,
    buyer_email     VARCHAR(255),
    buyer_phone     VARCHAR(20) NOT NULL,
    message         TEXT,
    status          VARCHAR(20) NOT NULL DEFAULT 'NEW',
    seller_user_id  UUID REFERENCES users(id) ON DELETE SET NULL,
    ip              INET,
    user_agent      TEXT,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_leads_tenant ON leads(tenant_id, created_at DESC);
CREATE INDEX idx_leads_store ON leads(physical_store_id, created_at DESC);
CREATE INDEX idx_leads_product ON leads(product_id);
CREATE INDEX idx_leads_status ON leads(status);
