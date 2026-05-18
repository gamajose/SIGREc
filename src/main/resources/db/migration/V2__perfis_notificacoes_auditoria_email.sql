INSERT INTO regulacao_tfd.roles (nome, descricao)
VALUES ('ROLE_SOLICITANTE', 'Usuário responsável por criar e acompanhar solicitações')
ON CONFLICT (nome) DO NOTHING;

INSERT INTO regulacao_tfd.roles (nome, descricao)
VALUES ('ROLE_REGULADOR', 'Usuário responsável pela regulação das solicitações')
ON CONFLICT (nome) DO NOTHING;

UPDATE regulacao_tfd.usuarios
SET role = 'SOLICITANTE'
WHERE role = 'COLABORADOR';

UPDATE regulacao_tfd.user_roles ur
SET role_id = (SELECT id FROM regulacao_tfd.roles WHERE nome = 'ROLE_SOLICITANTE')
WHERE role_id = (SELECT id FROM regulacao_tfd.roles WHERE nome = 'ROLE_COLABORADOR');

CREATE TABLE IF NOT EXISTS regulacao_tfd.notificacoes (
    id BIGSERIAL PRIMARY KEY,
    usuario_id BIGINT NOT NULL REFERENCES regulacao_tfd.usuarios(id),
    titulo VARCHAR(160) NOT NULL,
    mensagem TEXT NOT NULL,
    link VARCHAR(300),
    lida BOOLEAN NOT NULL DEFAULT FALSE,
    criado_em TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_notificacoes_usuario_lida
ON regulacao_tfd.notificacoes (usuario_id, lida, criado_em DESC);

CREATE TABLE IF NOT EXISTS regulacao_tfd.auditoria_eventos (
    id BIGSERIAL PRIMARY KEY,
    usuario_id BIGINT REFERENCES regulacao_tfd.usuarios(id),
    entidade VARCHAR(80) NOT NULL,
    entidade_id BIGINT,
    acao VARCHAR(80) NOT NULL,
    detalhes TEXT,
    criado_em TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_auditoria_eventos_entidade
ON regulacao_tfd.auditoria_eventos (entidade, entidade_id, criado_em DESC);

CREATE INDEX IF NOT EXISTS idx_auditoria_eventos_usuario
ON regulacao_tfd.auditoria_eventos (usuario_id, criado_em DESC);

CREATE TABLE IF NOT EXISTS regulacao_tfd.configuracoes_email (
    id BIGSERIAL PRIMARY KEY,
    host VARCHAR(200),
    porta INTEGER DEFAULT 587,
    usuario VARCHAR(200),
    senha VARCHAR(300),
    remetente VARCHAR(200),
    ativo BOOLEAN NOT NULL DEFAULT FALSE,
    usar_tls BOOLEAN NOT NULL DEFAULT TRUE,
    atualizado_em TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

INSERT INTO regulacao_tfd.configuracoes_email
    (host, porta, usuario, senha, remetente, ativo, usar_tls)
SELECT '', 587, '', '', '', false, true
WHERE NOT EXISTS (
    SELECT 1 FROM regulacao_tfd.configuracoes_email
);