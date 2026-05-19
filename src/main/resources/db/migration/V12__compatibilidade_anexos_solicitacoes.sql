SET search_path TO regulacao_tfd;

ALTER TABLE solicitacao_anexos
    ADD COLUMN IF NOT EXISTS nome_arquivo VARCHAR(260),
    ADD COLUMN IF NOT EXISTS tipo_documento VARCHAR(80),
    ADD COLUMN IF NOT EXISTS tamanho_bytes BIGINT,
    ADD COLUMN IF NOT EXISTS caminho_arquivo VARCHAR(500),
    ADD COLUMN IF NOT EXISTS usuario_upload BIGINT REFERENCES usuarios(id);

UPDATE solicitacao_anexos
SET nome_arquivo = nome_original
WHERE nome_arquivo IS NULL;

UPDATE solicitacao_anexos
SET tipo_documento = categoria
WHERE tipo_documento IS NULL;
