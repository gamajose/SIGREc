CREATE TABLE IF NOT EXISTS regulacao_tfd.notificacoes (
    id BIGSERIAL PRIMARY KEY,
    usuario_id BIGINT NOT NULL REFERENCES regulacao_tfd.usuarios(id) ON DELETE CASCADE,
    titulo VARCHAR(180) NOT NULL,
    mensagem TEXT NOT NULL,
    link VARCHAR(400),
    lida BOOLEAN NOT NULL DEFAULT FALSE,
    lida_em TIMESTAMP,
    criado_em TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_notificacoes_usuario_lida
    ON regulacao_tfd.notificacoes (usuario_id, lida, criado_em DESC);

CREATE INDEX IF NOT EXISTS idx_notificacoes_usuario_criado
    ON regulacao_tfd.notificacoes (usuario_id, criado_em DESC);
