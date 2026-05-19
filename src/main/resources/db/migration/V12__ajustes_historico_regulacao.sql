SET search_path TO regulacao_tfd;

ALTER TABLE solicitacao_historico
    ADD COLUMN IF NOT EXISTS setor VARCHAR(120),
    ADD COLUMN IF NOT EXISTS acao VARCHAR(40);

UPDATE solicitacao_historico
SET acao = 'MOVIMENTACAO'
WHERE acao IS NULL;
