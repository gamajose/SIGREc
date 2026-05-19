SET search_path TO regulacao_tfd;

CREATE TABLE IF NOT EXISTS solicitacao_anexos (
    id BIGSERIAL PRIMARY KEY,
    solicitacao_id BIGINT NOT NULL REFERENCES solicitacoes(id) ON DELETE CASCADE,
    categoria VARCHAR(60) NOT NULL DEFAULT 'OUTROS',
    nome_original VARCHAR(260) NOT NULL,
    content_type VARCHAR(120),
    criado_em TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_solicitacao_anexos_solicitacao
    ON solicitacao_anexos(solicitacao_id);
