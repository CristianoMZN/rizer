-- Planos comercializáveis. Seeding dos 3 planos iniciais.
CREATE TABLE plans (
    code                    VARCHAR(40) PRIMARY KEY,                   -- BASIC | PRO | PLATINUM
    name                    VARCHAR(120) NOT NULL,
    description             TEXT,
    max_physical_stores     INTEGER,                                   -- NULL = ilimitado
    has_partner_page        BOOLEAN NOT NULL DEFAULT FALSE,
    has_custom_domain       BOOLEAN NOT NULL DEFAULT FALSE,
    has_instagram           BOOLEAN NOT NULL DEFAULT FALSE,
    has_meta_dpa            BOOLEAN NOT NULL DEFAULT FALSE,
    has_google_shopping     BOOLEAN NOT NULL DEFAULT FALSE,
    price_cents             BIGINT NOT NULL,
    currency                CHAR(3) NOT NULL DEFAULT 'BRL',
    trial_days              INTEGER NOT NULL DEFAULT 0,
    stripe_price_id         VARCHAR(120),                              -- NULL até configurar no Stripe
    is_active               BOOLEAN NOT NULL DEFAULT TRUE,
    sort_order              INTEGER NOT NULL DEFAULT 0,
    created_at              TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at              TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

INSERT INTO plans (code, name, description, max_physical_stores, has_partner_page, has_custom_domain,
                   has_instagram, has_meta_dpa, has_google_shopping, price_cents, currency, trial_days, sort_order)
VALUES
('BASIC',    'Básico',    'Para quem está começando. 1 loja, anúncios ilimitados, sem página de parceiro.',
 1,    FALSE, FALSE, FALSE, FALSE, FALSE, 9900,  'BRL', 7,  10),
('PRO',      'PRO',       'Para redes e concessionárias. Até 3 lojas, página de parceiro, domínio customizado, integração com Instagram.',
 3,    TRUE,  TRUE,  TRUE,  FALSE, FALSE, 24900, 'BRL', 14, 20),
('PLATINUM', 'Platinum',  'Tudo do PRO + META Dynamic Product Ads + Google Shopping. Lojas ilimitadas.',
 NULL, TRUE,  TRUE,  TRUE,  TRUE,  TRUE,  59900, 'BRL', 14, 30)
ON CONFLICT (code) DO UPDATE SET
    name = EXCLUDED.name,
    description = EXCLUDED.description,
    max_physical_stores = EXCLUDED.max_physical_stores,
    has_partner_page = EXCLUDED.has_partner_page,
    has_custom_domain = EXCLUDED.has_custom_domain,
    has_instagram = EXCLUDED.has_instagram,
    has_meta_dpa = EXCLUDED.has_meta_dpa,
    has_google_shopping = EXCLUDED.has_google_shopping,
    price_cents = EXCLUDED.price_cents,
    currency = EXCLUDED.currency,
    trial_days = EXCLUDED.trial_days,
    sort_order = EXCLUDED.sort_order;

-- Assinatura do tenant. 1 ativa por tenant.
CREATE TYPE subscription_status AS ENUM (
    'trialing', 'active', 'past_due', 'paused', 'unpaid', 'canceled', 'incomplete', 'incomplete_expired'
);

CREATE TYPE subscription_source AS ENUM ('stripe', 'manual', 'trial', 'comp', 'courtesy');

CREATE TABLE subscriptions (
    id                       UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id                UUID NOT NULL UNIQUE REFERENCES tenants(id) ON DELETE CASCADE,
    plan_code                VARCHAR(40) NOT NULL REFERENCES plans(code),
    status                   subscription_status NOT NULL,
    currency                 CHAR(3) NOT NULL DEFAULT 'BRL',
    current_period_start     TIMESTAMPTZ NOT NULL,
    current_period_end       TIMESTAMPTZ NOT NULL,
    trial_start              TIMESTAMPTZ,
    trial_end                TIMESTAMPTZ,
    cancel_at_period_end     BOOLEAN NOT NULL DEFAULT FALSE,
    canceled_at              TIMESTAMPTZ,
    grace_period_days        INTEGER NOT NULL DEFAULT 7,
    stripe_customer_id       VARCHAR(120),
    stripe_subscription_id   VARCHAR(120),
    source                   subscription_source NOT NULL DEFAULT 'stripe',
    notes                    TEXT,
    created_at               TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at               TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_subs_status ON subscriptions(status);
CREATE INDEX idx_subs_plan ON subscriptions(plan_code);
CREATE INDEX idx_subs_period_end ON subscriptions(current_period_end);
