SET search_path TO regulacao_tfd;

CREATE EXTENSION IF NOT EXISTS postgres_fdw;

CREATE SCHEMA IF NOT EXISTS externo;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_foreign_server
        WHERE srvname = 'tb_procedimento_srv'
    ) THEN
        CREATE SERVER tb_procedimento_srv
        FOREIGN DATA WRAPPER postgres_fdw
        OPTIONS (
            host '127.0.0.1',
            port '5432',
            dbname 'tb_procedimento'
        );
    END IF;
END $$;

CREATE OR REPLACE VIEW regulacao_tfd.vw_procedimentos_externos AS
SELECT
    p.id::varchar AS codigo,
    p.descricao::text AS descricao,
    p.cod_identificacao::text AS cod_identificacao,
    p.valor_bruto::text AS valor_bruto,
    COALESCE(
        NULLIF(TRIM(p.valor_reais::text), '')::numeric,
        NULLIF(TRIM(p.valor_bruto::text), '')::numeric,
        0
    ) AS valor,
    NULL::varchar AS cod_servico,
    NULL::text AS linha_original,
    'SIGTAP'::varchar AS origem,
    true AS ativo
FROM externo.procedimentos p;

CREATE OR REPLACE VIEW regulacao_tfd.vw_procedimentos_unificados AS
SELECT
    e.codigo::varchar AS procedimento_id,
    e.codigo::varchar AS codigo,
    e.descricao::text AS descricao,
    'SIGTAP'::varchar AS tipo,
    e.valor::numeric AS valor,
    e.ativo::boolean AS ativo,
    e.origem::varchar AS origem
FROM regulacao_tfd.vw_procedimentos_externos e

UNION ALL

SELECT
    p.procedimento_id::varchar AS procedimento_id,
    p.codigo::varchar AS codigo,
    p.descricao::text AS descricao,
    p.tipo::varchar AS tipo,
    p.valor::numeric AS valor,
    p.ativo::boolean AS ativo,
    'referencia'::varchar AS origem
FROM regulacao_tfd.procedimentos p;