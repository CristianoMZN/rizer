-- 1) Campos de consumidor final no User (registro público)
ALTER TABLE users
    ADD COLUMN cpf                VARCHAR(14),
    ADD COLUMN birth_date         DATE,
    ADD COLUMN profile_completed  BOOLEAN NOT NULL DEFAULT FALSE;

-- CPF único apenas quando preenchido (e usuário não deletado)
CREATE UNIQUE INDEX uk_users_cpf ON users(cpf) WHERE cpf IS NOT NULL AND deleted_at IS NULL;

-- 2) Favoritos do consumidor (lista pessoal por usuário)
CREATE TABLE favorites (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id     UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    product_id  UUID NOT NULL REFERENCES products(id) ON DELETE CASCADE,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE (user_id, product_id)
);

CREATE INDEX idx_favorites_user_recent ON favorites(user_id, created_at DESC);
