-- Contexto regional (país) usado para resolver moeda, locale e regras locais.
-- Substitui o conceito ambíguo de "store" do esquema.tabelas.md original.
CREATE TABLE countries (
    code                     CHAR(2) PRIMARY KEY,            -- ISO 3166-1 alfa-2 (ex.: BR, US)
    name                     VARCHAR(120) NOT NULL,          -- Nome em inglês
    local_name               VARCHAR(120) NOT NULL,          -- Nome local (ex.: Brasil)
    iso_alpha_3              CHAR(3) NOT NULL,               -- ISO 3166-1 alfa-3 (ex.: BRA)
    numeric_code             CHAR(3) NOT NULL,               -- ISO 3166-1 numeric (ex.: 076)
    currency_code_iso        CHAR(3) NOT NULL,               -- ISO 4217 (ex.: BRL)
    currency_name            VARCHAR(80) NOT NULL,
    currency_symbol          VARCHAR(8) NOT NULL,
    currency_symbol_position VARCHAR(8) NOT NULL DEFAULT 'start',  -- start | end
    currency_minor_unit      SMALLINT NOT NULL DEFAULT 2,    -- casas decimais
    timezone_default         VARCHAR(64) NOT NULL,
    language_default         VARCHAR(8) NOT NULL,            -- ex.: pt-BR
    locale_default           VARCHAR(16) NOT NULL,           -- ex.: pt_BR
    default_phone_code       VARCHAR(8) NOT NULL,            -- ex.: 55
    date_format_default      VARCHAR(32) NOT NULL DEFAULT 'dd/MM/yyyy',
    postal_code_required     BOOLEAN NOT NULL DEFAULT TRUE,
    tax_identifier_label     VARCHAR(40) NOT NULL,           -- ex.: CPF, NIF, EIN
    address_format           VARCHAR(32) NOT NULL DEFAULT 'national',  -- national | international
    is_active                BOOLEAN NOT NULL DEFAULT TRUE,
    created_at               TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at               TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Seed: Brasil como país padrão nesta fase.
INSERT INTO countries (
    code, name, local_name, iso_alpha_3, numeric_code,
    currency_code_iso, currency_name, currency_symbol, currency_symbol_position, currency_minor_unit,
    timezone_default, language_default, locale_default, default_phone_code,
    date_format_default, postal_code_required, tax_identifier_label, address_format
) VALUES (
    'BR', 'Brazil', 'Brasil', 'BRA', '076',
    'BRL', 'Real Brasileiro', 'R$', 'start', 2,
    'America/Sao_Paulo', 'pt-BR', 'pt_BR', '55',
    'dd/MM/yyyy', TRUE, 'CPF/CNPJ', 'national'
) ON CONFLICT (code) DO NOTHING;

CREATE INDEX idx_countries_active ON countries(is_active);
