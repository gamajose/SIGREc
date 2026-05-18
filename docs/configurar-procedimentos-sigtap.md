# Configuração da base SIGTAP externa

O SIGREc usa a view `regulacao_tfd.vw_procedimentos_unificados` para listar procedimentos.

Quando a base SIGTAP estiver em outro banco PostgreSQL, configurar no banco `segrec`:

```sql
CREATE EXTENSION IF NOT EXISTS postgres_fdw;

CREATE SERVER IF NOT EXISTS srv_tb_procedimento
FOREIGN DATA WRAPPER postgres_fdw
OPTIONS (
    host 'IP_DO_POSTGRES',
    port '5432',
    dbname 'tb_procedimento'
);

CREATE USER MAPPING IF NOT EXISTS FOR jose
SERVER srv_tb_procedimento
OPTIONS (
    user 'jose',
    password 'SENHA'
);

CREATE SCHEMA IF NOT EXISTS externo;

IMPORT FOREIGN SCHEMA public
LIMIT TO (procedimentos)
FROM SERVER srv_tb_procedimento
INTO externo;

DROP VIEW IF EXISTS regulacao_tfd.vw_procedimentos_unificados;

CREATE VIEW regulacao_tfd.vw_procedimentos_unificados AS
SELECT
    codigo,
    descricao,
    tipo,
    valor,
    ativo,
    origem
FROM regulacao_tfd.procedimentos

UNION ALL

SELECT
    id AS codigo,
    descricao,
    NULL::varchar AS tipo,
    valor_reais AS valor,
    true AS ativo,
    'SIGTAP'::varchar AS origem
FROM externo.procedimentos;