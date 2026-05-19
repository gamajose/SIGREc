SET search_path TO regulacao_tfd;

ALTER TABLE tickets_suporte
ADD COLUMN IF NOT EXISTS numero_ticket varchar(30),
ADD COLUMN IF NOT EXISTS descricao_html text,
ADD COLUMN IF NOT EXISTS prioridade varchar(20) DEFAULT 'NORMAL',
ADD COLUMN IF NOT EXISTS categoria varchar(80),
ADD COLUMN IF NOT EXISTS atualizado_em timestamp DEFAULT now();

UPDATE tickets_suporte
SET numero_ticket = 'TCK-' || LPAD(id::text, 6, '0')
WHERE numero_ticket IS NULL;

ALTER TABLE tickets_suporte
ALTER COLUMN numero_ticket SET NOT NULL;

CREATE UNIQUE INDEX IF NOT EXISTS uk_tickets_suporte_numero_ticket
ON tickets_suporte (numero_ticket);

CREATE INDEX IF NOT EXISTS idx_tickets_suporte_status
ON tickets_suporte (status);

CREATE INDEX IF NOT EXISTS idx_tickets_suporte_criado_em
ON tickets_suporte (criado_em);

CREATE INDEX IF NOT EXISTS idx_tickets_suporte_usuario_id
ON tickets_suporte (usuario_id);

CREATE TABLE IF NOT EXISTS ticket_anexos (
    id bigserial PRIMARY KEY,
    ticket_id bigint NOT NULL REFERENCES tickets_suporte(id) ON DELETE CASCADE,
    nome_arquivo varchar(255) NOT NULL,
    nome_original varchar(255) NOT NULL,
    content_type varchar(120),
    tamanho_bytes bigint,
    caminho_arquivo text NOT NULL,
    criado_em timestamp DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_ticket_anexos_ticket_id
ON ticket_anexos(ticket_id);