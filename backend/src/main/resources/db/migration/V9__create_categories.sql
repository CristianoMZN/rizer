-- Categorias hierárquicas (até 3 níveis) via ltree.
-- Raiz = reino do veículo (carro, moto, caminhão, náutico, ônibus).
-- Sub-níveis = tipo (hatch, sedã, SUV...) e opcional modelo genérico.
CREATE TABLE categories (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    country_code    CHAR(2) NOT NULL REFERENCES countries(code),
    realm           VARCHAR(40) NOT NULL,            -- CAR | MOTORCYCLE | TRUCK | NAUTICAL | BUS  (espelha vehicle_brands.vehicle_type)
    path            ltree NOT NULL,                  -- ex.: 'carros.carros_passeio.hatch'
    name            VARCHAR(120) NOT NULL,
    slug            VARCHAR(120) NOT NULL,
    parent_id       UUID REFERENCES categories(id),
    level           SMALLINT NOT NULL CHECK (level BETWEEN 1 AND 3),
    sort_order      INTEGER NOT NULL DEFAULT 0,
    icon            VARCHAR(60),
    image_url       VARCHAR(512),
    description     TEXT,
    is_active       BOOLEAN NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE (country_code, path),
    UNIQUE (country_code, realm, slug)
);

CREATE INDEX idx_categories_path_gist ON categories USING GIST (path);
CREATE INDEX idx_categories_path_btree ON categories USING BTREE (path);
CREATE INDEX idx_categories_realm ON categories(country_code, realm) WHERE is_active = TRUE;
CREATE INDEX idx_categories_parent ON categories(parent_id);

-- ─── Seed: Brasil ────────────────────────────────────────────────────────────

-- Nível 1: reinos
INSERT INTO categories (country_code, realm, path, name, slug, parent_id, level, sort_order, icon, description)
VALUES
    ('BR', 'CAR',         'carros',         'Carros',         'carros',         NULL, 1, 10, 'directions_car',  'Automóveis de passeio'),
    ('BR', 'MOTORCYCLE',  'motos',          'Motos',          'motos',          NULL, 1, 20, 'two_wheeler',     'Motocicletas'),
    ('BR', 'TRUCK',       'caminhoes',      'Caminhões',      'caminhoes',      NULL, 1, 30, 'local_shipping',  'Caminhões e comerciais leves'),
    ('BR', 'NAUTICAL',    'nauticos',       'Náuticos',       'nauticos',       NULL, 1, 40, 'sailing',         'Embarcações'),
    ('BR', 'BUS',         'onibus',         'Ônibus',         'onibus',         NULL, 1, 50, 'directions_bus',  'Ônibus e micro-ônibus')
ON CONFLICT (country_code, path) DO NOTHING;

-- Nível 2: tipos. Path = 'reino.tipo' (ltree com 2 labels).
-- Insere primeiro todos os reinos com seus filhos via subselect no pai.
INSERT INTO categories (country_code, realm, path, name, slug, parent_id, level, sort_order, icon, description)
SELECT 'BR', 'CAR', sub.path::ltree, sub.name, sub.slug, p.id, 2, sub.ord, sub.icon, sub.description
FROM (VALUES
    ('carros.hatch',        'Hatch',          'hatch',        1, NULL,           'Compacto, porta traseira vertical'),
    ('carros.seda',         'Sedã',           'seda',         2, NULL,           'Carroceria sedã tradicional'),
    ('carros.suv',          'SUV',            'suv',          3, NULL,           'Utilitário esportivo'),
    ('carros.pickup',       'Pickup',         'pickup',       4, NULL,           'Picape com caçamba'),
    ('carros.cupe',         'Cupê',           'cupe',         5, NULL,           'Carroceria cupê'),
    ('carros.perua',        'Perua/Station',  'perua',        6, NULL,           'Station wagon'),
    ('carros.conversivel',  'Conversível',    'conversivel',  7, NULL,           'Capota retrátil'),
    ('carros.van',          'Van/Utilitário', 'van',          8, NULL,           'Van e utilitário leve')
) AS sub(path, name, slug, ord, icon, description)
JOIN categories p ON p.country_code = 'BR' AND p.path = sub.path::ltree
WHERE nlevel(sub.path::ltree) = 2
ON CONFLICT (country_code, path) DO NOTHING;

INSERT INTO categories (country_code, realm, path, name, slug, parent_id, level, sort_order, icon, description)
SELECT 'BR', 'MOTORCYCLE', sub.path::ltree, sub.name, sub.slug, p.id, 2, sub.ord, sub.icon, sub.description
FROM (VALUES
    ('motos.urban',   'Urbana',     'urban',   1, NULL, 'Moto urbana / scooter'),
    ('motos.naked',   'Naked',      'naked',   2, NULL, 'Esportiva sem carenagem'),
    ('motos.sport',   'Esportiva',  'sport',   3, NULL, 'Carenada de alta performance'),
    ('motos.custom',  'Custom',     'custom',  4, NULL, 'Custom / cruiser'),
    ('motos.trail',   'Trail',      'trail',   5, NULL, 'Off-road / adventure'),
    ('motos.scooter', 'Scooter',    'scooter', 6, NULL, 'Scooter automático')
) AS sub(path, name, slug, ord, icon, description)
JOIN categories p ON p.country_code = 'BR' AND p.path = sub.path::ltree
ON CONFLICT (country_code, path) DO NOTHING;

INSERT INTO categories (country_code, realm, path, name, slug, parent_id, level, sort_order, icon, description)
SELECT 'BR', 'TRUCK', sub.path::ltree, sub.name, sub.slug, p.id, 2, sub.ord, sub.icon, sub.description
FROM (VALUES
    ('caminhoes.leve',  'Leve',           'leve',    1, NULL, 'VUC / caminhão leve'),
    ('caminhoes.medio', 'Médio',          'medio',   2, NULL, 'Toco / truck médio'),
    ('caminhoes.pesado','Pesado',         'pesado',  3, NULL, 'Carreta pesado'),
    ('caminhoes.cavalo','Cavalo mecânico','cavalo',  4, NULL, 'Cavalo mecânico')
) AS sub(path, name, slug, ord, icon, description)
JOIN categories p ON p.country_code = 'BR' AND p.path = sub.path::ltree
ON CONFLICT (country_code, path) DO NOTHING;

INSERT INTO categories (country_code, realm, path, name, slug, parent_id, level, sort_order, icon, description)
SELECT 'BR', 'NAUTICAL', sub.path::ltree, sub.name, sub.slug, p.id, 2, sub.ord, sub.icon, sub.description
FROM (VALUES
    ('nauticos.lancha',  'Lancha',  'lancha',  1, NULL, 'Lancha'),
    ('nauticos.jet',     'Jet ski', 'jet',     2, NULL, 'Jet ski / moto aquática'),
    ('nauticos.veleiro', 'Veleiro', 'veleiro', 3, NULL, 'Veleiro'),
    ('nauticos.barco',   'Barco',   'barco',   4, NULL, 'Barco de pesca / recreio')
) AS sub(path, name, slug, ord, icon, description)
JOIN categories p ON p.country_code = 'BR' AND p.path = sub.path::ltree
ON CONFLICT (country_code, path) DO NOTHING;

INSERT INTO categories (country_code, realm, path, name, slug, parent_id, level, sort_order, icon, description)
SELECT 'BR', 'BUS', sub.path::ltree, sub.name, sub.slug, p.id, 2, sub.ord, sub.icon, sub.description
FROM (VALUES
    ('onibus.urbano',     'Urbano',        'urbano',     1, NULL, 'Ônibus urbano'),
    ('onibus.rodoviario', 'Rodoviário',    'rodoviario', 2, NULL, 'Ônibus rodoviário'),
    ('onibus.micro',      'Micro-ônibus',  'micro',      3, NULL, 'Micro-ônibus / van')
) AS sub(path, name, slug, ord, icon, description)
JOIN categories p ON p.country_code = 'BR' AND p.path = sub.path::ltree
ON CONFLICT (country_code, path) DO NOTHING;
