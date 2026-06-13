-- Tabela de marcas de veículos (dados FIPE)
CREATE TABLE vehicle_brands (
    id              SERIAL PRIMARY KEY,
    vehicle_type    VARCHAR(20) NOT NULL,
    fipe_id         INTEGER NOT NULL,
    name            VARCHAR(100) NOT NULL,
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    UNIQUE (vehicle_type, fipe_id)
);

CREATE INDEX idx_vehicle_brands_type ON vehicle_brands(vehicle_type);
CREATE INDEX idx_vehicle_brands_name ON vehicle_brands(name);

-- Tabela de modelos de veículos (dados FIPE)
CREATE TABLE vehicle_models (
    id              SERIAL PRIMARY KEY,
    brand_id        INTEGER NOT NULL REFERENCES vehicle_brands(id),
    fipe_id         INTEGER NOT NULL,
    name            VARCHAR(150) NOT NULL,
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    UNIQUE (brand_id, fipe_id)
);

CREATE INDEX idx_vehicle_models_brand ON vehicle_models(brand_id);
CREATE INDEX idx_vehicle_models_name ON vehicle_models(name);
