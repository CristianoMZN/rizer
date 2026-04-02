INSERT INTO categories (slug, name, description)
VALUES
    ('veiculos', 'Veiculos', 'Categoria para automoveis, motos e utilitarios'),
    ('imoveis', 'Imoveis', 'Categoria para venda e locacao de imoveis')
ON CONFLICT (slug) DO NOTHING;

INSERT INTO subcategories (category_id, slug, name, description)
SELECT c.id, v.slug, v.name, v.description
FROM categories c
JOIN (
    VALUES
        ('veiculos', 'carros', 'Carros', 'Veiculos de passeio e utilitarios leves'),
        ('veiculos', 'motos', 'Motos', 'Motocicletas e scooters'),
        ('imoveis', 'residencial', 'Residencial', 'Casas, apartamentos e coberturas')
) AS v(category_slug, slug, name, description)
ON c.slug = v.category_slug
ON CONFLICT (category_id, slug) DO NOTHING;

INSERT INTO subsubcategories (subcategory_id, slug, name, description)
SELECT sc.id, v.slug, v.name, v.description
FROM subcategories sc
JOIN (
    VALUES
        ('carros', 'suv', 'SUV', 'Utilitarios esportivos'),
        ('carros', 'sedan', 'Sedan', 'Sedans compactos e executivos'),
        ('motos', 'street', 'Street', 'Motos para uso urbano'),
        ('residencial', 'apartamento', 'Apartamento', 'Unidades em condominio')
) AS v(subcategory_slug, slug, name, description)
ON sc.slug = v.subcategory_slug
ON CONFLICT (subcategory_id, slug) DO NOTHING;

INSERT INTO users (id, email, name, avatar_url, provider, provider_id, created_at, updated_at)
VALUES
    ('c9f9d2ee-9c22-4f96-8ca5-3ff2dbf88230', 'vendedor1@riser.local', 'Loja Prime Veiculos', NULL, 'seed', 'seed-vendedor-1', NOW(), NOW()),
    ('18c766a0-4dac-4d9b-8e55-2f4b31fd5f90', 'vendedor2@riser.local', 'Riser Imoveis Centro', NULL, 'seed', 'seed-vendedor-2', NOW(), NOW())
ON CONFLICT (email) DO NOTHING;

INSERT INTO sellers (user_id, display_name, legal_name, tax_id, phone, email, created_at, updated_at)
VALUES
    ('c9f9d2ee-9c22-4f96-8ca5-3ff2dbf88230', 'Loja Prime Veiculos', 'Prime Comercio de Veiculos LTDA', '11111111000111', '+55 54 99999-1000', 'contato@primeveiculos.local', NOW(), NOW()),
    ('18c766a0-4dac-4d9b-8e55-2f4b31fd5f90', 'Riser Imoveis Centro', 'Riser Negocios Imobiliarios LTDA', '22222222000122', '+55 54 99999-2000', 'contato@riserimoveis.local', NOW(), NOW())
ON CONFLICT (tax_id) DO NOTHING;

INSERT INTO products (uuid, merchant_id, realm, category_path, attributes, seller_id, subsubcategory_id, created_at, updated_at)
SELECT
    p.uuid,
    p.merchant_id,
    p.realm,
    p.category_path::ltree,
    p.attributes::jsonb,
    s.id,
    ssc.id,
    NOW(),
    NOW()
FROM (
    VALUES
        (
            'a6d0f14b-3ec1-4d6c-b5ef-80a57d6e6501'::uuid,
            'c9f9d2ee-9c22-4f96-8ca5-3ff2dbf88230'::uuid,
            'VEHICLES',
            'veiculos.carros.suv',
            '{"ano": 2022, "quilometragem": 32500, "combustivel": "Flex", "cambio": "Automatico"}',
            'Loja Prime Veiculos',
            'suv'
        ),
        (
            '5a13e574-a74f-49e3-a094-30ac4efde476'::uuid,
            '18c766a0-4dac-4d9b-8e55-2f4b31fd5f90'::uuid,
            'REAL_ESTATE',
            'imoveis.residencial.apartamento',
            '{"quartos": 2, "banheiros": 1, "area_m2": 68, "vagas": 1}',
            'Riser Imoveis Centro',
            'apartamento'
        )
) AS p(uuid, merchant_id, realm, category_path, attributes, seller_name, subsubcategory_slug)
JOIN sellers s ON s.display_name = p.seller_name
JOIN subsubcategories ssc ON ssc.slug = p.subsubcategory_slug
ON CONFLICT (uuid) DO NOTHING;

INSERT INTO product_localizations (
    product_id,
    country_code,
    title,
    description,
    price,
    currency,
    unit_system,
    location,
    created_at,
    updated_at
)
SELECT
    pr.id,
    l.country_code,
    l.title,
    l.description,
    l.price,
    l.currency,
    l.unit_system,
    ST_SetSRID(ST_MakePoint(l.lon, l.lat), 4326),
    NOW(),
    NOW()
FROM products pr
JOIN (
    VALUES
        (
            'a6d0f14b-3ec1-4d6c-b5ef-80a57d6e6501'::uuid,
            'BR',
            'SUV seminovo 2022',
            'SUV completo, revisoes em dia e procedencia garantida',
            129900.00::numeric,
            'BRL',
            'METRIC',
            -52.2737::double precision,
            -27.1004::double precision
        ),
        (
            '5a13e574-a74f-49e3-a094-30ac4efde476'::uuid,
            'BR',
            'Apartamento 2 dormitorios no centro',
            'Imovel com excelente iluminacao e pronto para morar',
            420000.00::numeric,
            'BRL',
            'METRIC',
            -52.2037::double precision,
            -28.2636::double precision
        )
) AS l(product_uuid, country_code, title, description, price, currency, unit_system, lon, lat)
ON pr.uuid = l.product_uuid
ON CONFLICT (product_id, country_code) DO NOTHING;
