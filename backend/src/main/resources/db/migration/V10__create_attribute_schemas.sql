-- Schema dinâmico (JSON Schema draft-07 simplificado) para validar
-- o campo `products.attributes` por (country_code, categoria).
CREATE TABLE attribute_schemas (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    country_code        CHAR(2) NOT NULL REFERENCES countries(code),
    realm               VARCHAR(40),                                -- null = aplica a todos os reinos
    category_path       ltree NOT NULL,                             -- 'carros.*' cobre todos os sub-carros
    entity_type         VARCHAR(40) NOT NULL,                       -- 'product' | 'physical_store' | 'tenant'
    version             INTEGER NOT NULL DEFAULT 1,
    is_active           BOOLEAN NOT NULL DEFAULT TRUE,
    schema_definition   JSONB NOT NULL,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE (country_code, entity_type, realm, category_path, version)
);

CREATE INDEX idx_attribute_schemas_lookup
    ON attribute_schemas(country_code, entity_type, category_path, is_active);

-- Seed: schema genérico de produto/veículo (Brasil).
-- Aplicado a qualquer categoria que não tenha schema mais específico.
INSERT INTO attribute_schemas (country_code, realm, category_path, entity_type, version, schema_definition)
VALUES
('BR', NULL, 'carros', 'product', 1, '{
  "$schema": "http://json-schema.org/draft-07/schema#",
  "type": "object",
  "required": ["mileage_km", "year_model", "year_build", "fuel", "transmission"],
  "properties": {
    "mileage_km":      { "type": "integer", "minimum": 0, "maximum": 2000000, "description": "Quilometragem atual" },
    "year_model":      { "type": "integer", "minimum": 1950, "maximum": 2100 },
    "year_build":      { "type": "integer", "minimum": 1950, "maximum": 2100 },
    "fuel":            { "type": "string", "enum": ["Flex", "Gasolina", "Álcool", "Diesel", "Elétrico", "Híbrido"] },
    "transmission":    { "type": "string", "enum": ["Manual", "Automático", "Automatizado", "CVT"] },
    "color":           { "type": "string", "maxLength": 60 },
    "doors":           { "type": "integer", "minimum": 2, "maximum": 5 },
    "engine":          { "type": "string", "maxLength": 40, "description": "Ex.: 1.0, 2.0 Turbo" },
    "vin":             { "type": "string", "minLength": 17, "maxLength": 17, "description": "Chassi" },
    "license_plate":   { "type": "string", "pattern": "^[A-Z]{3}[0-9][A-Z0-9][0-9]{2}$" },
    "previous_owners": { "type": "integer", "minimum": 0, "maximum": 50 },
    "features":        { "type": "array", "items": { "type": "string", "maxLength": 60 }, "maxItems": 50 },
    "warranty_months": { "type": "integer", "minimum": 0, "maximum": 120 },
    "bluetooth":       { "type": "boolean" },
    "gps":             { "type": "boolean" },
    "leather_seats":   { "type": "boolean" }
  },
  "additionalProperties": true
}'::jsonb),

('BR', NULL, 'motos', 'product', 1, '{
  "type": "object",
  "required": ["mileage_km", "year_model", "year_build"],
  "properties": {
    "mileage_km":    { "type": "integer", "minimum": 0, "maximum": 500000 },
    "year_model":    { "type": "integer", "minimum": 1950, "maximum": 2100 },
    "year_build":    { "type": "integer", "minimum": 1950, "maximum": 2100 },
    "fuel":          { "type": "string", "enum": ["Gasolina", "Flex", "Elétrico", "Híbrido"] },
    "engine_cc":     { "type": "integer", "minimum": 50, "maximum": 3000 },
    "color":         { "type": "string", "maxLength": 60 },
    "start_type":    { "type": "string", "enum": ["Elétrico", "Pedal", "Elétrico/Pedal"] },
    "features":      { "type": "array", "items": { "type": "string" }, "maxItems": 30 }
  },
  "additionalProperties": true
}'::jsonb),

('BR', NULL, 'caminhoes', 'product', 1, '{
  "type": "object",
  "required": ["mileage_km", "year_model", "year_build"],
  "properties": {
    "mileage_km":     { "type": "integer", "minimum": 0, "maximum": 5000000 },
    "year_model":     { "type": "integer", "minimum": 1950, "maximum": 2100 },
    "year_build":     { "type": "integer", "minimum": 1950, "maximum": 2100 },
    "fuel":           { "type": "string", "enum": ["Diesel", "Gasolina", "Flex", "Elétrico", "Híbrido"] },
    "pbt_kg":         { "type": "integer", "minimum": 0, "maximum": 200000 },
    "cargo_capacity": { "type": "integer", "minimum": 0, "maximum": 200000 },
    "axles":          { "type": "integer", "minimum": 2, "maximum": 9 }
  },
  "additionalProperties": true
}'::jsonb),

('BR', NULL, 'nauticos', 'product', 1, '{
  "type": "object",
  "required": ["year_model"],
  "properties": {
    "year_model":  { "type": "integer", "minimum": 1950, "maximum": 2100 },
    "length_m":    { "type": "number", "minimum": 0, "maximum": 100 },
    "engine_hp":   { "type": "integer", "minimum": 0, "maximum": 5000 },
    "engine_type": { "type": "string", "enum": ["Popa", "Centro", "Jet", "Vela", "Elétrico"] },
    "fuel":        { "type": "string", "enum": ["Gasolina", "Diesel", "Elétrico"] },
    "capacity":    { "type": "integer", "minimum": 1, "maximum": 500 }
  },
  "additionalProperties": true
}'::jsonb),

('BR', NULL, 'onibus', 'product', 1, '{
  "type": "object",
  "required": ["year_model", "mileage_km"],
  "properties": {
    "mileage_km":  { "type": "integer", "minimum": 0, "maximum": 5000000 },
    "year_model":  { "type": "integer", "minimum": 1950, "maximum": 2100 },
    "year_build":  { "type": "integer", "minimum": 1950, "maximum": 2100 },
    "fuel":        { "type": "string", "enum": ["Diesel", "Gasolina", "Elétrico", "Híbrido"] },
    "seats":       { "type": "integer", "minimum": 4, "maximum": 90 },
    "has_ac":      { "type": "boolean" },
    "has_wifi":    { "type": "boolean" },
    "has_bathroom":{ "type": "boolean" }
  },
  "additionalProperties": true
}'::jsonb)

ON CONFLICT (country_code, entity_type, realm, category_path, version) DO NOTHING;
