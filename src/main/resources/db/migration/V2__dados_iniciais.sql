SET search_path TO regulacao_tfd;

INSERT INTO roles (nome, descricao) VALUES
('ROLE_ADMIN', 'Administrador geral'),
('ROLE_SOLICITANTE', 'Solicitante de unidade'),
('ROLE_REGULACAO', 'Equipe de regulacao/secretaria'),
('ROLE_AUTORIZADOR', 'Autorizador'),
('ROLE_AUDITORIA', 'Consulta e auditoria')
ON CONFLICT (nome) DO NOTHING;

INSERT INTO unidades_saude (nome, cnes, municipio, uf, telefone, responsavel)
VALUES ('Secretaria Municipal de Saude', '0000000', 'Municipio', 'AL', '', 'Administrador')
ON CONFLICT DO NOTHING;

INSERT INTO usuarios (nome, username, senha_hash, unidade_id)
SELECT 'Administrador', 'admin', '$2a$10$8c0lZA6Wv7dP1eMyd1sOjuowDmhRcqXyd7gYM8AIkWIXUkDkcc5v2', u.id
FROM unidades_saude u
WHERE u.nome = 'Secretaria Municipal de Saude'
ON CONFLICT (username) DO NOTHING;

INSERT INTO user_roles (usuario_id, role_id)
SELECT u.id, r.id FROM usuarios u CROSS JOIN roles r
WHERE u.username = 'admin'
ON CONFLICT DO NOTHING;

INSERT INTO procedimentos (codigo, descricao, tipo, valor, origem) VALUES
('0203010086', 'EXAME CITOPATOLOGICO CERVICO VAGINAL/MICROFLORA-RASTREAMENTO', 'APAC', 14.37, 'referencia'),
('0203020030', 'EXAME ANATOMO-PATOLOGICO PARA CONGELAMENTO/PARAFINA POR PECA CIRURGICA OU POR BIOPSIA', 'APAC', 40.78, 'referencia')
ON CONFLICT (codigo) DO NOTHING;

INSERT INTO cid10 (codigo, descricao) VALUES
('Z75.3', 'Indisponibilidade e inacessibilidade de servicos de saude'),
('R69', 'Causas desconhecidas e nao especificadas de morbidade')
ON CONFLICT (codigo) DO NOTHING;
