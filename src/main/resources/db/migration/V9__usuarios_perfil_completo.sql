ALTER TABLE regulacao_tfd.usuarios
    ADD COLUMN IF NOT EXISTS codigo_usuario VARCHAR(40),
    ADD COLUMN IF NOT EXISTS telefone VARCHAR(40),
    ADD COLUMN IF NOT EXISTS unidade_telefone VARCHAR(40),
    ADD COLUMN IF NOT EXISTS unidade_endereco VARCHAR(240),
    ADD COLUMN IF NOT EXISTS unidade_cep VARCHAR(12),
    ADD COLUMN IF NOT EXISTS unidade_numero VARCHAR(30),
    ADD COLUMN IF NOT EXISTS unidade_bairro VARCHAR(120),
    ADD COLUMN IF NOT EXISTS unidade_complemento VARCHAR(160),
    ADD COLUMN IF NOT EXISTS perfil_completo BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS primeiro_login BOOLEAN NOT NULL DEFAULT TRUE,
    ADD COLUMN IF NOT EXISTS data_perfil_atualizado TIMESTAMP;

CREATE UNIQUE INDEX IF NOT EXISTS uk_usuarios_codigo_usuario
    ON regulacao_tfd.usuarios (codigo_usuario)
    WHERE codigo_usuario IS NOT NULL AND codigo_usuario <> '';

CREATE INDEX IF NOT EXISTS idx_usuarios_unidade_id
    ON regulacao_tfd.usuarios (unidade_id);

UPDATE regulacao_tfd.usuarios
SET codigo_usuario = username
WHERE codigo_usuario IS NULL OR codigo_usuario = '';
