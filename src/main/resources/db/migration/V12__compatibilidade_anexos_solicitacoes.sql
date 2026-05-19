SET search_path TO regulacao_tfd;

ALTER TABLE solicitacao_anexos
    ADD COLUMN IF NOT EXISTS nome_arquivo VARCHAR(260),
    ADD COLUMN IF NOT EXISTS tipo_documento VARCHAR(80),
    ADD COLUMN IF NOT EXISTS tamanho_bytes BIGINT,
    ADD COLUMN IF NOT EXISTS caminho_arquivo VARCHAR(500),
    ADD COLUMN IF NOT EXISTS usuario_upload BIGINT REFERENCES usuarios(id);

UPDATE solicitacao_anexos
SET nome_arquivo = coalesce(nome_arquivo, 'anexo')
WHERE nome_arquivo IS NULL;

UPDATE solicitacao_anexos
SET tipo_documento = coalesce(tipo_documento, categoria, 'Outros')
WHERE tipo_documento IS NULL;
