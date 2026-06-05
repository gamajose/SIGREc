DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM pg_foreign_server
        WHERE srvname = 'srv_tb_procedimento'
    ) THEN
        ALTER SERVER srv_tb_procedimento
        OPTIONS (SET host '192.168.3.200', SET port '5432', SET dbname 'tb_procedimento');
    END IF;

    IF EXISTS (
        SELECT 1
        FROM pg_foreign_server
        WHERE srvname = 'tb_procedimento_srv'
    ) THEN
        ALTER SERVER tb_procedimento_srv
        OPTIONS (SET host '192.168.3.200', SET port '5432', SET dbname 'tb_procedimento');
    END IF;
END $$;
