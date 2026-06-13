-- Histórico de checagens do CNAME de custom domain.
-- Útil para debug e auditoria ("quando o cliente configurou o CNAME e o que falhou").
CREATE TABLE custom_domain_checks (
    id                    UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id             UUID NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    domain                VARCHAR(255) NOT NULL,
    expected_target       VARCHAR(255) NOT NULL,                    -- slug.motorise.com.br
    cname_found           VARCHAR(255),
    resolved_ip          VARCHAR(64),
    status                VARCHAR(20) NOT NULL,                       -- VERIFIED | FAILED | PENDING
    error_message         TEXT,
    checked_at            TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_custom_domain_checks_tenant ON custom_domain_checks(tenant_id, checked_at DESC);
CREATE INDEX idx_custom_domain_checks_domain ON custom_domain_checks(domain);
