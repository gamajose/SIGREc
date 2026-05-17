SET search_path TO regulacao_tfd;

ALTER TABLE usuarios
    ADD COLUMN IF NOT EXISTS email VARCHAR(180),
    ADD COLUMN IF NOT EXISTS password_hash VARCHAR(120),
    ADD COLUMN IF NOT EXISTS role VARCHAR(30) NOT NULL DEFAULT 'COLABORADOR',
    ADD COLUMN IF NOT EXISTS created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP;

UPDATE usuarios
SET password_hash = senha_hash
WHERE password_hash IS NULL;

UPDATE usuarios
SET role = 'ADMIN'
WHERE username = 'admin';

INSERT INTO roles (nome, descricao) VALUES
('ROLE_COLABORADOR', 'Colaborador solicitante')
ON CONFLICT (nome) DO NOTHING;

INSERT INTO user_roles (usuario_id, role_id)
SELECT u.id, r.id
FROM usuarios u
JOIN roles r ON r.nome = 'ROLE_COLABORADOR'
WHERE u.role = 'COLABORADOR'
ON CONFLICT DO NOTHING;

CREATE TABLE IF NOT EXISTS usuario_auditoria (
    id BIGSERIAL PRIMARY KEY,
    usuario_alvo_id BIGINT REFERENCES usuarios(id),
    usuario_acao_id BIGINT REFERENCES usuarios(id),
    acao VARCHAR(80) NOT NULL,
    detalhes TEXT,
    criado_em TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE OR REPLACE VIEW users AS
SELECT
    id,
    username,
    email,
    password_hash,
    role,
    created_at,
    updated_at
FROM usuarios;
