SET search_path TO regulacao_tfd;

CREATE TABLE IF NOT EXISTS ticket_interacoes (
    id BIGSERIAL PRIMARY KEY,
    ticket_id BIGINT NOT NULL REFERENCES tickets_suporte(id) ON DELETE CASCADE,
    usuario_id BIGINT REFERENCES usuarios(id),
    tipo VARCHAR(40) NOT NULL DEFAULT 'COMENTARIO',
    mensagem TEXT NOT NULL,
    mensagem_html TEXT,
    criado_em TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_ticket_interacoes_ticket_id
    ON ticket_interacoes(ticket_id);

CREATE TABLE IF NOT EXISTS notificacoes_sistema (
    id BIGSERIAL PRIMARY KEY,
    usuario_id BIGINT REFERENCES usuarios(id) ON DELETE CASCADE,
    titulo VARCHAR(180) NOT NULL,
    mensagem TEXT NOT NULL,
    url VARCHAR(240),
    lida BOOLEAN NOT NULL DEFAULT FALSE,
    criado_em TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    lida_em TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_notificacoes_sistema_usuario_lida
    ON notificacoes_sistema(usuario_id, lida);
