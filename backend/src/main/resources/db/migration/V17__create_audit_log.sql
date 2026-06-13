-- Auditoria de ações sensíveis (LGPD art. 37, segurança, compliance).
-- Armazenado por 5 anos.
CREATE TABLE audit_log (
    id              BIGSERIAL PRIMARY KEY,
    actor_user_id   UUID REFERENCES users(id) ON DELETE SET NULL,
    tenant_id       UUID REFERENCES tenants(id) ON DELETE SET NULL,
    action          VARCHAR(80) NOT NULL,                -- 'tenant.create', 'subscription.change_status', 'auth.login', etc.
    resource_type   VARCHAR(60),                          -- 'tenant', 'subscription', 'payment', 'integration'
    resource_id     VARCHAR(64),                          -- UUID ou external id
    severity        VARCHAR(20) NOT NULL DEFAULT 'INFO', -- INFO | WARN | ERROR
    payload         JSONB,                                -- before/after/diff arbitrário
    ip              INET,
    user_agent      TEXT,
    correlation_id  VARCHAR(64),                          -- request-id propagado do gateway
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_audit_actor ON audit_log(actor_user_id, created_at DESC);
CREATE INDEX idx_audit_tenant ON audit_log(tenant_id, created_at DESC);
CREATE INDEX idx_audit_action ON audit_log(action, created_at DESC);
CREATE INDEX idx_audit_severity ON audit_log(severity, created_at DESC) WHERE severity IN ('WARN', 'ERROR');
CREATE INDEX idx_audit_correlation ON audit_log(correlation_id);
