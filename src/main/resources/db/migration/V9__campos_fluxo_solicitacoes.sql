SET search_path TO regulacao_tfd;

ALTER TABLE solicitacoes
    ADD COLUMN IF NOT EXISTS numero_solicitacao VARCHAR(30),
    ADD COLUMN IF NOT EXISTS prioridade VARCHAR(20) NOT NULL DEFAULT 'NORMAL',
    ADD COLUMN IF NOT EXISTS justificativa_clinica TEXT,
    ADD COLUMN IF NOT EXISTS parecer_regulacao TEXT,
    ADD COLUMN IF NOT EXISTS status_anterior VARCHAR(40),
    ADD COLUMN IF NOT EXISTS enviado_em TIMESTAMP,
    ADD COLUMN IF NOT EXISTS analisado_em TIMESTAMP,
    ADD COLUMN IF NOT EXISTS atualizado_em TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP;

UPDATE solicitacoes
SET numero_solicitacao = 'SOL-' || lpad(id::text, 6, '0')
WHERE numero_solicitacao IS NULL;

CREATE UNIQUE INDEX IF NOT EXISTS uk_solicitacoes_numero_solicitacao
    ON solicitacoes(numero_solicitacao);
