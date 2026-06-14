-- Livro-caixa: registra toda entrada (Stripe + manual).
-- payment_methods cobre Stripe (cartão, Pix, boleto) e manual (dinheiro, transferência, etc).
CREATE TYPE payment_method AS ENUM (
    'stripe_card',
    'stripe_pix',
    'stripe_boleto',
    'manual_cash',
    'manual_bank_transfer',
    'manual_pix_external',
    'manual_bonus',
    'manual_courtesy',
    'manual_other'
);

CREATE TYPE payment_status AS ENUM (
    'pending', 'succeeded', 'failed', 'refunded', 'voided', 'chargeback'
);

CREATE TABLE payments (
    id                       UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id                UUID NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    subscription_id          UUID REFERENCES subscriptions(id) ON DELETE SET NULL,
    method                   payment_method NOT NULL,
    status                   payment_status NOT NULL,
    amount_cents             BIGINT NOT NULL,
    currency                 VARCHAR(3) NOT NULL DEFAULT 'BRL',
    period_start             TIMESTAMPTZ,
    period_end               TIMESTAMPTZ,
    description              TEXT,
    external_reference       VARCHAR(255),                                -- stripe_pi_…, txid pix, NSU
    receipt_url              TEXT,                                        -- URL pública do comprovante (S3)
    paid_at                  TIMESTAMPTZ,
    recorded_by_user_id      UUID REFERENCES users(id),                  -- NULL quando veio do Stripe
    notes                    TEXT,
    created_at               TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at               TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_payments_tenant_paid_at ON payments(tenant_id, paid_at DESC);
CREATE INDEX idx_payments_status_paid_at ON payments(status, paid_at DESC);
CREATE INDEX idx_payments_method ON payments(method);
CREATE INDEX idx_payments_subscription ON payments(subscription_id);

-- Invoices do Stripe (1 payment pode referenciar 1 invoice; ou 1 invoice pode ser referenciada por N attempts).
CREATE TABLE stripe_invoices (
    id                       UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    payment_id               UUID NOT NULL REFERENCES payments(id) ON DELETE CASCADE,
    stripe_invoice_id        VARCHAR(120) NOT NULL UNIQUE,
    stripe_charge_id         VARCHAR(120),
    hosted_invoice_url       TEXT,
    invoice_pdf              TEXT,
    amount_due_cents         BIGINT,
    amount_paid_cents        BIGINT,
    raw_payload              JSONB,
    created_at               TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_stripe_invoices_payment ON stripe_invoices(payment_id);
