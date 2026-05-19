ALTER TABLE regulacao_tfd.solicitacoes
ADD COLUMN IF NOT EXISTS procedimento_codigo_externo VARCHAR(20);

DROP VIEW IF EXISTS regulacao_tfd.vw_procedimentos_unificados;

CREATE VIEW regulacao_tfd.vw_procedimentos_unificados AS
SELECT
    codigo::varchar AS codigo,
    descricao::text AS descricao,
    tipo::varchar AS tipo,
    valor::numeric(12,2) AS valor,
    ativo::boolean AS ativo,
    origem::varchar AS origem
FROM regulacao_tfd.procedimentos;