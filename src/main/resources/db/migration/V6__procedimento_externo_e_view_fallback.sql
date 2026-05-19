ALTER TABLE regulacao_tfd.solicitacoes
ADD COLUMN IF NOT EXISTS procedimento_codigo_externo VARCHAR(20);

CREATE OR REPLACE VIEW regulacao_tfd.vw_procedimentos_unificados AS
SELECT
    codigo,
    descricao,
    tipo,
    valor,
    ativo,
    origem
FROM regulacao_tfd.procedimentos;