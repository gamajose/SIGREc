CREATE SCHEMA IF NOT EXISTS regulacao_tfd;
SET search_path TO regulacao_tfd;

CREATE TABLE roles (
    id BIGSERIAL PRIMARY KEY,
    nome VARCHAR(60) NOT NULL UNIQUE,
    descricao VARCHAR(180) NOT NULL
);

CREATE TABLE usuarios (
    id BIGSERIAL PRIMARY KEY,
    nome VARCHAR(140) NOT NULL,
    username VARCHAR(80) NOT NULL UNIQUE,
    senha_hash VARCHAR(120) NOT NULL,
    unidade_id BIGINT,
    ativo BOOLEAN NOT NULL DEFAULT TRUE,
    criado_em TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    ultimo_login TIMESTAMP
);

CREATE TABLE user_roles (
    usuario_id BIGINT NOT NULL REFERENCES usuarios(id),
    role_id BIGINT NOT NULL REFERENCES roles(id),
    PRIMARY KEY (usuario_id, role_id)
);

CREATE TABLE unidades_saude (
    id BIGSERIAL PRIMARY KEY,
    nome VARCHAR(180) NOT NULL,
    cnes VARCHAR(20),
    municipio VARCHAR(120) NOT NULL,
    uf CHAR(2) NOT NULL,
    telefone VARCHAR(40),
    responsavel VARCHAR(140),
    ativo BOOLEAN NOT NULL DEFAULT TRUE
);

ALTER TABLE usuarios ADD CONSTRAINT fk_usuario_unidade FOREIGN KEY (unidade_id) REFERENCES unidades_saude(id);

CREATE TABLE profissionais (
    id BIGSERIAL PRIMARY KEY,
    nome VARCHAR(160) NOT NULL,
    cpf_cns VARCHAR(20),
    conselho VARCHAR(20),
    registro_conselho VARCHAR(40),
    especialidade VARCHAR(120),
    unidade_id BIGINT REFERENCES unidades_saude(id),
    solicitante BOOLEAN NOT NULL DEFAULT TRUE,
    autorizador BOOLEAN NOT NULL DEFAULT FALSE,
    ativo BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE pacientes (
    id BIGSERIAL PRIMARY KEY,
    nome VARCHAR(180) NOT NULL,
    cns VARCHAR(20),
    cpf VARCHAR(20),
    rg VARCHAR(30),
    data_nascimento DATE,
    sexo VARCHAR(20),
    raca_cor VARCHAR(40),
    nome_mae VARCHAR(180),
    endereco VARCHAR(240),
    municipio VARCHAR(120),
    uf CHAR(2),
    cep VARCHAR(12),
    telefone VARCHAR(40),
    email VARCHAR(140),
    responsavel VARCHAR(160),
    criado_em TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_pacientes_nome ON pacientes (nome);
CREATE INDEX idx_pacientes_cns ON pacientes (cns);
CREATE INDEX idx_pacientes_cpf ON pacientes (cpf);

CREATE TABLE acompanhantes (
    id BIGSERIAL PRIMARY KEY,
    paciente_id BIGINT REFERENCES pacientes(id),
    nome VARCHAR(180) NOT NULL,
    data_nascimento DATE,
    cpf VARCHAR(20),
    telefone VARCHAR(40),
    parentesco VARCHAR(80)
);

CREATE TABLE procedimentos (
    id BIGSERIAL PRIMARY KEY,
    codigo VARCHAR(20) NOT NULL UNIQUE,
    descricao VARCHAR(300) NOT NULL,
    tipo VARCHAR(60),
    valor NUMERIC(12,2),
    ativo BOOLEAN NOT NULL DEFAULT TRUE,
    origem VARCHAR(80)
);

CREATE INDEX idx_procedimentos_descricao ON procedimentos USING gin (to_tsvector('portuguese', descricao));

CREATE TABLE cid10 (
    id BIGSERIAL PRIMARY KEY,
    codigo VARCHAR(10) NOT NULL UNIQUE,
    descricao VARCHAR(240) NOT NULL,
    ativo BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE solicitacoes (
    id BIGSERIAL PRIMARY KEY,
    numero_protocolo VARCHAR(30) NOT NULL UNIQUE,
    tipo_solicitacao VARCHAR(10) NOT NULL CHECK (tipo_solicitacao IN ('TFD','APAC')),
    paciente_id BIGINT NOT NULL REFERENCES pacientes(id),
    unidade_solicitante_id BIGINT NOT NULL REFERENCES unidades_saude(id),
    profissional_solicitante_id BIGINT REFERENCES profissionais(id),
    profissional_autorizador_id BIGINT REFERENCES profissionais(id),
    procedimento_principal_id BIGINT REFERENCES procedimentos(id),
    cid10_principal VARCHAR(10),
    cid10_secundario VARCHAR(10),
    descricao_diagnostico TEXT,
    justificativa TEXT,
    prioridade VARCHAR(20) NOT NULL DEFAULT 'NORMAL',
    status VARCHAR(40) NOT NULL DEFAULT 'RASCUNHO',
    data_entrada TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    data_envio TIMESTAMP,
    data_analise TIMESTAMP,
    data_autorizacao TIMESTAMP,
    data_impressao TIMESTAMP,
    usuario_criacao BIGINT REFERENCES usuarios(id),
    usuario_ultima_alteracao BIGINT REFERENCES usuarios(id),
    observacao_regulacao TEXT,
    motivo_indeferimento TEXT,
    ativo BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE INDEX idx_solicitacoes_fila ON solicitacoes (status, prioridade, data_entrada);
CREATE INDEX idx_solicitacoes_tipo ON solicitacoes (tipo_solicitacao);

CREATE TABLE solicitacao_tfd (
    solicitacao_id BIGINT PRIMARY KEY REFERENCES solicitacoes(id) ON DELETE CASCADE,
    tratamentos_previos TEXT,
    procedimento_exame_indicado TEXT,
    sinais_sintomas TEXT,
    necessita_acompanhante BOOLEAN NOT NULL DEFAULT FALSE,
    acompanhante_nome VARCHAR(180),
    acompanhante_data_nascimento DATE,
    acompanhante_cpf VARCHAR(20),
    acompanhante_telefone VARCHAR(40),
    destino VARCHAR(180),
    justificativa_medica TEXT
);

CREATE TABLE solicitacao_apac (
    solicitacao_id BIGINT PRIMARY KEY REFERENCES solicitacoes(id) ON DELETE CASCADE,
    prontuario VARCHAR(40),
    raca_cor VARCHAR(40),
    responsavel VARCHAR(160),
    ibge VARCHAR(10),
    quantidade INTEGER NOT NULL DEFAULT 1,
    causas_associadas TEXT,
    observacoes TEXT,
    data_solicitacao DATE,
    numero_apac VARCHAR(40),
    validade_inicio DATE,
    validade_fim DATE,
    estabelecimento_executante VARCHAR(180),
    cnes_executante VARCHAR(20)
);

CREATE TABLE solicitacao_procedimentos (
    id BIGSERIAL PRIMARY KEY,
    solicitacao_id BIGINT NOT NULL REFERENCES solicitacoes(id) ON DELETE CASCADE,
    procedimento_id BIGINT NOT NULL REFERENCES procedimentos(id),
    papel VARCHAR(30) NOT NULL DEFAULT 'SECUNDARIO',
    quantidade INTEGER NOT NULL DEFAULT 1
);

CREATE TABLE solicitacao_anexos (
    id BIGSERIAL PRIMARY KEY,
    solicitacao_id BIGINT NOT NULL REFERENCES solicitacoes(id) ON DELETE CASCADE,
    tipo_documento VARCHAR(80) NOT NULL,
    nome_arquivo VARCHAR(240) NOT NULL,
    caminho_storage VARCHAR(400) NOT NULL,
    content_type VARCHAR(120),
    tamanho BIGINT,
    usuario_upload BIGINT REFERENCES usuarios(id),
    criado_em TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE solicitacao_historico (
    id BIGSERIAL PRIMARY KEY,
    solicitacao_id BIGINT NOT NULL REFERENCES solicitacoes(id) ON DELETE CASCADE,
    usuario_id BIGINT REFERENCES usuarios(id),
    setor VARCHAR(100),
    status_anterior VARCHAR(40),
    status_novo VARCHAR(40),
    acao VARCHAR(80) NOT NULL,
    observacao TEXT,
    criado_em TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE autorizacoes (
    id BIGSERIAL PRIMARY KEY,
    solicitacao_id BIGINT NOT NULL REFERENCES solicitacoes(id) ON DELETE CASCADE,
    numero_autorizacao VARCHAR(40) NOT NULL,
    numero_apac VARCHAR(40),
    validade_inicio DATE,
    validade_fim DATE,
    profissional_autorizador_id BIGINT REFERENCES profissionais(id),
    usuario_autorizador_id BIGINT REFERENCES usuarios(id),
    criado_em TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE impressoes (
    id BIGSERIAL PRIMARY KEY,
    solicitacao_id BIGINT NOT NULL REFERENCES solicitacoes(id) ON DELETE CASCADE,
    usuario_id BIGINT REFERENCES usuarios(id),
    tipo_formulario VARCHAR(20) NOT NULL,
    reimpressao BOOLEAN NOT NULL DEFAULT FALSE,
    caminho_storage VARCHAR(400) NOT NULL,
    hash_arquivo VARCHAR(128),
    criado_em TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE parametros_sistema (
    chave VARCHAR(100) PRIMARY KEY,
    valor TEXT NOT NULL
);

CREATE TABLE auditoria_login (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(80) NOT NULL,
    sucesso BOOLEAN NOT NULL,
    ip VARCHAR(60),
    user_agent VARCHAR(300),
    criado_em TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
