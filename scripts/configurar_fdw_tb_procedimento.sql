-- Script de configuração do FDW para acesso ao banco tb_procedimento
-- Executar conectado no banco principal do SIGREc, exemplo: segrec
-- Ajustar usuário e senha antes de executar.

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
            host '192.168.42.128',
            port '5432',
            dbname 'tb_procedimento'
        );
    END IF;
END $$;

DROP USER MAPPING IF EXISTS FOR CURRENT_USER SERVER tb_procedimento_srv;

CREATE USER MAPPING FOR CURRENT_USER
SERVER tb_procedimento_srv
OPTIONS (
    user 'jose',
    password 'Joseluiz1'
);

CREATE SCHEMA IF NOT EXISTS externo;

DROP FOREIGN TABLE IF EXISTS externo.procedimentos;

IMPORT FOREIGN SCHEMA public
LIMIT TO (procedimentos)
FROM SERVER tb_procedimento_srv
INTO externo;

SELECT count(*) AS total_procedimentos_importados
FROM externo.procedimentos;