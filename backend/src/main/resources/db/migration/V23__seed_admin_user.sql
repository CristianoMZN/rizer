-- Seed do usuário administrador global da plataforma.
-- Senha padrão: Admin123! (deve ser alterada no primeiro acesso via /auth/login)
INSERT INTO users (email, name, password_hash, provider, system_role, is_active)
VALUES (
    'admin@motorise.com.br',
    'Administrator',
    '$2a$10$EN0Cz9TfgX4ymFEpcoKjPeZEIBANr4ODXKFZddxPa3FfCrePO5F8y',
    'local',
    'sys_admin',
    true
)
ON CONFLICT (email) DO NOTHING;
