SET search_path TO regulacao_tfd;

CREATE TABLE IF NOT EXISTS solicitacao_historico (
    id BIGSERIAL PRIMARY KEY,
    solicitacao_id BIGINT NOT NULL REFERENCES solicitacoes(id) ON DELETE CASCADE,
    usuario_id BIGINT REFERENCES usuarios(id),
    status_anterior VARCHAR(40),
    status_novo VARCHAR(40),
    tipo VARCHAR(40) NOT NULL DEFAULT 'MOVIMENTACAO',
    observacao TEXT,
    criado_em TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_solicitacao_historico_solicitacao
    ON solicitacao_historico(solicitacao_id);
