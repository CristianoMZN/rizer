-- LGPD: consentimentos do titular dos dados.
-- 'user_id' é NULL para visitantes anônimos (que consentem via cookie).
-- 'document_version' muda quando os termos são atualizados, forçando
-- re-consentimento do titular.
CREATE TYPE consent_purpose AS ENUM (
    'terms_of_use',
    'privacy_policy',
    'cookies_essential',
    'cookies_analytics',
    'cookies_marketing',
    'marketing_emails',
    'data_sharing_integrations'
);

CREATE TABLE consents (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id             UUID REFERENCES users(id) ON DELETE CASCADE,
    anonymous_id        VARCHAR(64),                                  -- cookie uuid quando não logado
    purpose             consent_purpose NOT NULL,
    granted             BOOLEAN NOT NULL,
    ip                  INET,
    user_agent          TEXT,
    document_version    VARCHAR(20) NOT NULL,                         -- "v1.0", "v1.1", ...
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_consents_user ON consents(user_id, purpose, created_at DESC);
CREATE INDEX idx_consents_anon ON consents(anonymous_id, purpose, created_at DESC);
CREATE INDEX idx_consents_purpose_version ON consents(purpose, document_version);

-- Solicitações de exportação de dados (direito do titular — art. 18 LGPD).
-- O backend gera um pacote ZIP/JSON com todos os dados do user e armazena
-- em S3 com link presigned válido por 7 dias.
CREATE TYPE data_export_status AS ENUM (
    'pending',
    'processing',
    'ready',
    'expired',
    'failed'
);

CREATE TABLE data_export_requests (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id             UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    status              data_export_status NOT NULL DEFAULT 'pending',
    storage_key         VARCHAR(512),                                -- S3 key do arquivo gerado
    download_url        TEXT,                                         -- presigned URL (válida 7 dias)
    url_expires_at      TIMESTAMPTZ,
    error_message       TEXT,
    requested_at        TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    completed_at        TIMESTAMPTZ
);

CREATE INDEX idx_data_export_user ON data_export_requests(user_id, requested_at DESC);
CREATE INDEX idx_data_export_status ON data_export_requests(status);
