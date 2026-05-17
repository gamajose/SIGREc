SET search_path TO regulacao_tfd;

ALTER TABLE usuarios
    ADD COLUMN IF NOT EXISTS endereco VARCHAR(240);

ALTER TABLE unidades_saude
    ADD COLUMN IF NOT EXISTS tipo_unidade VARCHAR(30) NOT NULL DEFAULT 'AMBAS';

ALTER TABLE solicitacoes
    ADD COLUMN IF NOT EXISTS unidade_autorizadora_id BIGINT REFERENCES unidades_saude(id);

UPDATE solicitacoes s
SET unidade_autorizadora_id = (
    SELECT id FROM unidades_saude
    WHERE tipo_unidade IN ('AUTORIZADORA', 'AMBAS')
    ORDER BY id
    LIMIT 1
)
WHERE unidade_autorizadora_id IS NULL;

CREATE TABLE IF NOT EXISTS tickets_suporte (
    id BIGSERIAL PRIMARY KEY,
    usuario_id BIGINT NOT NULL REFERENCES usuarios(id),
    assunto VARCHAR(160) NOT NULL,
    mensagem TEXT NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'ABERTO',
    resposta TEXT,
    email_destino VARCHAR(180),
    respondido_por BIGINT REFERENCES usuarios(id),
    criado_em TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    respondido_em TIMESTAMP
);

CREATE TABLE IF NOT EXISTS email_outbox (
    id BIGSERIAL PRIMARY KEY,
    destino VARCHAR(180) NOT NULL,
    assunto VARCHAR(180) NOT NULL,
    corpo TEXT NOT NULL,
    enviado BOOLEAN NOT NULL DEFAULT FALSE,
    criado_em TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    enviado_em TIMESTAMP
);
