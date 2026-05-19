package br.gov.sigrec.tfdapac.service;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.sql.Date;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class SolicitacaoService {

    private final JdbcTemplate jdbcTemplate;
    private final ProtocolService protocolService;
    private final NotificationService notificationService;
    private final AuditEventService auditEventService;

    public SolicitacaoService(JdbcTemplate jdbcTemplate,
            ProtocolService protocolService,
            NotificationService notificationService,
            AuditEventService auditEventService) {
        this.jdbcTemplate = jdbcTemplate;
        this.protocolService = protocolService;
        this.notificationService = notificationService;
        this.auditEventService = auditEventService;
    }

    public List<Map<String, Object>> fila(String status, String tipo, String termo) {
        return jdbcTemplate.queryForList("""
                select s.*, p.nome paciente_nome, p.cns paciente_cns, u.nome unidade_nome, ua.nome unidade_autorizadora_nome, coalesce(pr.codigo, s.procedimento_codigo_externo) procedimento_codigo,
                coalesce(pr.descricao, vpu.descricao) procedimento_descricao
                from regulacao_tfd.solicitacoes s
                join regulacao_tfd.pacientes p on p.id = s.paciente_id
                join regulacao_tfd.unidades_saude u on u.id = s.unidade_solicitante_id
                left join regulacao_tfd.unidades_saude ua on ua.id = s.unidade_autorizadora_id
                left join regulacao_tfd.procedimentos pr on pr.id = s.procedimento_principal_id
                left join regulacao_tfd.vw_procedimentos_unificados vpu on vpu.codigo = s.procedimento_codigo_externo
                where (? = '' or s.status = ?)
                  and (? = '' or s.tipo_solicitacao = ?)
                  and (? = '' or p.nome ilike ? or s.numero_protocolo ilike ? or coalesce(pr.descricao, vpu.descricao, '') ilike ?)
                order by
                  case s.prioridade when 'URGENTE' then 1 when 'ALTA' then 2 when 'NORMAL' then 3 else 4 end,
                  s.data_entrada
                """, nvl(status), nvl(status), nvl(tipo), nvl(tipo), nvl(termo), like(termo), like(termo), like(termo));
    }

    public List<Map<String, Object>> filaDoUsuario(String status, String tipo, String termo, Long usuarioId, Long unidadeId, boolean admin) {
        if (admin) {
            return fila(status, tipo, termo);
        }
        return jdbcTemplate.queryForList("""
                select s.*, p.nome paciente_nome, p.cns paciente_cns, u.nome unidade_nome, ua.nome unidade_autorizadora_nome, coalesce(pr.codigo, s.procedimento_codigo_externo) procedimento_codigo,
                coalesce(pr.descricao, vpu.descricao) procedimento_descricao
                from regulacao_tfd.solicitacoes s
                join regulacao_tfd.pacientes p on p.id = s.paciente_id
                join regulacao_tfd.unidades_saude u on u.id = s.unidade_solicitante_id
                left join regulacao_tfd.unidades_saude ua on ua.id = s.unidade_autorizadora_id
                left join regulacao_tfd.procedimentos pr on pr.id = s.procedimento_principal_id
                left join regulacao_tfd.vw_procedimentos_unificados vpu on vpu.codigo = s.procedimento_codigo_externo
                where (? = '' or s.status = ?)
                  and (? = '' or s.tipo_solicitacao = ?)
                  and (? = '' or p.nome ilike ? or s.numero_protocolo ilike ? or coalesce(pr.descricao, vpu.descricao, '') ilike ?)
                  and (s.usuario_criacao = ? or (? is not null and s.unidade_solicitante_id = ?))
                order by
                  case s.prioridade when 'URGENTE' then 1 when 'ALTA' then 2 when 'NORMAL' then 3 else 4 end,
                  s.data_entrada
                """, nvl(status), nvl(status), nvl(tipo), nvl(tipo), nvl(termo), like(termo), like(termo), like(termo),
                usuarioId, unidadeId, unidadeId);
    }

    public Map<String, Object> detalhe(Long id) {
        return jdbcTemplate.queryForMap("""
                select s.*, p.nome paciente_nome, p.cns paciente_cns, p.cpf paciente_cpf, p.rg paciente_rg,
                       p.data_nascimento paciente_nascimento, p.sexo paciente_sexo, p.nome_mae paciente_mae,
                       p.endereco paciente_endereco, p.municipio paciente_municipio, p.uf paciente_uf, p.cep paciente_cep,
                       p.telefone paciente_telefone, p.email paciente_email,
                       u.nome unidade_nome, u.cnes unidade_cnes, u.municipio unidade_municipio, u.uf unidade_uf,
                       ua.nome unidade_autorizadora_nome, ua.cnes unidade_autorizadora_cnes,
                       ps.nome profissional_solicitante_nome, ps.cpf_cns profissional_solicitante_doc,
                       ps.conselho profissional_solicitante_conselho, ps.registro_conselho profissional_solicitante_registro,
                       pa.nome profissional_autorizador_nome,
                       coalesce(pr.codigo, s.procedimento_codigo_externo) procedimento_codigo,
                       coalesce(pr.descricao, vpu.descricao) procedimento_descricao
                from regulacao_tfd.solicitacoes s
                join regulacao_tfd.pacientes p on p.id = s.paciente_id
                join regulacao_tfd.unidades_saude u on u.id = s.unidade_solicitante_id
                left join regulacao_tfd.unidades_saude ua on ua.id = s.unidade_autorizadora_id
                left join regulacao_tfd.profissionais ps on ps.id = s.profissional_solicitante_id
                left join regulacao_tfd.profissionais pa on pa.id = s.profissional_autorizador_id
                left join regulacao_tfd.procedimentos pr on pr.id = s.procedimento_principal_id
                left join regulacao_tfd.vw_procedimentos_unificados vpu on vpu.codigo = s.procedimento_codigo_externo
                where s.id = ?
                """, id);
    }

    public boolean podeAcessar(Long id, Long usuarioId, Long unidadeId, boolean admin) {
        if (admin) {
            return true;
        }
        Integer total = jdbcTemplate.queryForObject("""
                select count(*)
                from regulacao_tfd.solicitacoes
                where id = ?
                  and (usuario_criacao = ? or (? is not null and unidade_solicitante_id = ?))
                """, Integer.class, id, usuarioId, unidadeId, unidadeId);
        return total != null && total > 0;
    }

    public Map<String, Object> tfd(Long id) {
        return jdbcTemplate.queryForMap("select * from regulacao_tfd.solicitacao_tfd where solicitacao_id = ?", id);
    }

    public Map<String, Object> apac(Long id) {
        return jdbcTemplate.queryForMap("select * from regulacao_tfd.solicitacao_apac where solicitacao_id = ?", id);
    }

    public List<Map<String, Object>> historico(Long id) {
        return jdbcTemplate.queryForList("""
                select h.*, u.nome usuario_nome
                from regulacao_tfd.solicitacao_historico h
                left join regulacao_tfd.usuarios u on u.id = h.usuario_id
                where h.solicitacao_id = ?
                order by h.criado_em desc
                """, id);
    }

    public List<Map<String, Object>> anexos(Long id) {
        return jdbcTemplate.queryForList("""
                select a.*, u.nome usuario_nome
                from regulacao_tfd.solicitacao_anexos a
                left join regulacao_tfd.usuarios u on u.id = a.usuario_upload
                where a.solicitacao_id = ?
                order by a.criado_em desc
                """, id);
    }

    @Transactional
    public Long criar(Map<String, String> form, Long usuarioId) {
        String tipo = required(form, "tipo_solicitacao");
        String protocolo = protocolService.next(tipo);
        Long id = jdbcTemplate.queryForObject("""
                insert into regulacao_tfd.solicitacoes
                (numero_protocolo, tipo_solicitacao, paciente_id, unidade_solicitante_id, unidade_autorizadora_id, profissional_solicitante_id,
                 procedimento_principal_id, procedimento_codigo_externo, cid10_principal, cid10_secundario, descricao_diagnostico, justificativa,
                 prioridade, status, data_envio, usuario_criacao, usuario_ultima_alteracao)
                values (?, ?, ?, ?, coalesce(?, (select id from regulacao_tfd.unidades_saude where tipo_unidade in ('AUTORIZADORA','AMBAS') order by id limit 1)), ?, ?, ?, ?, ?, ?, ?, ?, 'ENVIADA', current_timestamp, ?, ?)
                returning id
                """, Long.class,
                protocolo,
                tipo,
                longValue(form, "paciente_id"),
                longValue(form, "unidade_solicitante_id"),
                nullableLong(form, "unidade_autorizadora_id"),
                nullableLong(form, "profissional_solicitante_id"),
                procedimentoIdPorCodigoOuNull(form),
                procedimentoCodigoOuNull(form),
                form.get("cid10_principal"),
                form.get("cid10_secundario"),
                form.get("descricao_diagnostico"),
                form.get("justificativa"),
                form.getOrDefault("prioridade", "NORMAL"),
                usuarioId,
                usuarioId);
        if ("TFD".equals(tipo)) {
            jdbcTemplate.update("""
                    insert into regulacao_tfd.solicitacao_tfd
                    (solicitacao_id, tratamentos_previos, procedimento_exame_indicado, sinais_sintomas,
                     necessita_acompanhante, acompanhante_nome, acompanhante_data_nascimento, acompanhante_cpf,
                     acompanhante_telefone, destino, justificativa_medica)
                    values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                    """, id, form.get("tratamentos_previos"), form.get("procedimento_exame_indicado"),
                    form.get("sinais_sintomas"), "on".equals(form.get("necessita_acompanhante")),
                    form.get("acompanhante_nome"), date(form.get("acompanhante_data_nascimento"), "Data de nascimento do acompanhante"),
                    form.get("acompanhante_cpf"), form.get("acompanhante_telefone"),
                    form.get("destino"), form.get("justificativa_medica"));
        } else {
            jdbcTemplate.update("""
                    insert into regulacao_tfd.solicitacao_apac
                    (solicitacao_id, prontuario, raca_cor, responsavel, ibge, quantidade, causas_associadas,
                     observacoes, data_solicitacao, numero_apac, validade_inicio, validade_fim,
                     estabelecimento_executante, cnes_executante)
                    values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                    """, id, form.get("prontuario"), form.get("raca_cor"), form.get("responsavel"),
                    form.get("ibge"), intValue(form, "quantidade", 1), form.get("causas_associadas"),
                    form.get("observacoes"), date(form.get("data_solicitacao"), "Data da solicitação"),
                    form.get("numero_apac"), date(form.get("validade_inicio"), "Validade inicial"), date(form.get("validade_fim"), "Validade final"),
                    form.get("estabelecimento_executante"), form.get("cnes_executante"));
        }
        historico(id, usuarioId, null, "ENVIADA", "CRIACAO", "Solicitacao enviada para regulacao");
        auditEventService.registrar(
                usuarioId,
                "SOLICITACAO",
                id,
                "CRIACAO",
                "Solicitação enviada para regulação"
        );
        notificationService.notificarReguladores(
                "Nova solicitação aguardando regulação",
                "Uma nova solicitação foi enviada para análise da regulação.",
                "/regulacao/" + id
        );
        return id;
    }

    @Transactional
    public void atualizar(Long id, Map<String, String> form, Long usuarioId) {
        String status = jdbcTemplate.queryForObject("select status from regulacao_tfd.solicitacoes where id = ?", String.class, id);
        if (!"RASCUNHO".equals(status) && !"ENVIADA".equals(status) && !"DEVOLVIDA_CORRECAO".equals(status)) {
            throw new IllegalStateException("Somente requisicoes em rascunho, enviadas ou devolvidas podem ser editadas.");
        }
        jdbcTemplate.update("""
                update regulacao_tfd.solicitacoes
                set paciente_id = coalesce(?, paciente_id),
                    unidade_solicitante_id = coalesce(?, unidade_solicitante_id),
                    profissional_solicitante_id = ?,
                    procedimento_principal_id = ?,
                    cid10_principal = ?,
                    cid10_secundario = ?,
                    descricao_diagnostico = ?,
                    justificativa = ?,
                    prioridade = coalesce(?, prioridade),
                    usuario_ultima_alteracao = ?
                where id = ?
                """,
                nullableLong(form, "paciente_id"),
                nullableLong(form, "unidade_solicitante_id"),
                nullableLong(form, "profissional_solicitante_id"),
                nullableLong(form, "procedimento_principal_id"),
                form.get("cid10_principal"),
                form.get("cid10_secundario"),
                form.get("descricao_diagnostico"),
                form.get("justificativa"),
                form.get("prioridade"),
                usuarioId,
                id);
        String tipo = jdbcTemplate.queryForObject("select tipo_solicitacao from regulacao_tfd.solicitacoes where id = ?", String.class, id);
        if ("TFD".equals(tipo)) {
            jdbcTemplate.update("""
                    update regulacao_tfd.solicitacao_tfd
                    set tratamentos_previos = ?,
                        procedimento_exame_indicado = ?,
                        sinais_sintomas = ?,
                        necessita_acompanhante = ?,
                        acompanhante_nome = ?,
                        acompanhante_data_nascimento = ?,
                        acompanhante_cpf = ?,
                        acompanhante_telefone = ?,
                        destino = ?,
                        justificativa_medica = ?
                    where solicitacao_id = ?
                    """, form.get("tratamentos_previos"), form.get("procedimento_exame_indicado"),
                    form.get("sinais_sintomas"), booleanValue(form, "necessita_acompanhante"),
                    form.get("acompanhante_nome"), date(form.get("acompanhante_data_nascimento"), "Data de nascimento do acompanhante"),
                    form.get("acompanhante_cpf"), form.get("acompanhante_telefone"),
                    form.get("destino"), form.get("justificativa_medica"), id);
        } else {
            jdbcTemplate.update("""
                    update regulacao_tfd.solicitacao_apac
                    set prontuario = ?,
                        raca_cor = ?,
                        responsavel = ?,
                        ibge = ?,
                        quantidade = ?,
                        causas_associadas = ?,
                        observacoes = ?,
                        data_solicitacao = ?,
                        numero_apac = ?,
                        validade_inicio = ?,
                        validade_fim = ?,
                        estabelecimento_executante = ?,
                        cnes_executante = ?
                    where solicitacao_id = ?
                    """, form.get("prontuario"), form.get("raca_cor"), form.get("responsavel"),
                    form.get("ibge"), intValue(form, "quantidade", 1), form.get("causas_associadas"),
                    form.get("observacoes"), date(form.get("data_solicitacao"), "Data da solicitação"),
                    form.get("numero_apac"), date(form.get("validade_inicio"), "Validade inicial"), date(form.get("validade_fim"), "Validade final"),
                    form.get("estabelecimento_executante"), form.get("cnes_executante"), id);
        }
        historico(id, usuarioId, status, status, "EDICAO", form.getOrDefault("observacao", "Requisicao atualizada"));
    }

    @Transactional
    public void mudarStatus(Long id, String novoStatus, String observacao, Long usuarioId, Map<String, String> form) {
        String anterior = jdbcTemplate.queryForObject("select status from regulacao_tfd.solicitacoes where id = ?", String.class, id);
        validarTransicaoStatus(anterior, novoStatus);
        jdbcTemplate.update("""
                update regulacao_tfd.solicitacoes
                set status = ?,
                    observacao_regulacao = coalesce(?, observacao_regulacao),
                    motivo_indeferimento = case when ? = 'INDEFERIDA' then ? else motivo_indeferimento end,
                    profissional_autorizador_id = coalesce(?, profissional_autorizador_id),
                    data_analise = case when ? in ('EM_ANALISE','DEVOLVIDA_CORRECAO','AGUARDANDO_DOCUMENTOS') then current_timestamp else data_analise end,
                    data_autorizacao = case when ? = 'AUTORIZADA' then current_timestamp else data_autorizacao end,
                    usuario_ultima_alteracao = ?
                where id = ?
                """, novoStatus, observacao, novoStatus, observacao,
                nullableLong(form, "profissional_autorizador_id"), novoStatus, novoStatus, usuarioId, id);
        if ("AUTORIZADA".equals(novoStatus)) {
            jdbcTemplate.update("""
                    insert into regulacao_tfd.autorizacoes
                    (solicitacao_id, numero_autorizacao, numero_apac, validade_inicio, validade_fim,
                     profissional_autorizador_id, usuario_autorizador_id)
                    values (?, ?, ?, ?, ?, ?, ?)
                    """, id, form.getOrDefault("numero_autorizacao", "AUT-" + id), form.get("numero_apac"),
                    date(form.get("validade_inicio"), "Validade inicial"), date(form.get("validade_fim"), "Validade final"),
                    nullableLong(form, "profissional_autorizador_id"), usuarioId);
        }
        historico(id, usuarioId, anterior, novoStatus, "MUDANCA_STATUS", observacao);
        auditEventService.registrar(
                usuarioId,
                "SOLICITACAO",
                id,
                "MUDANCA_STATUS",
                "Status alterado de " + anterior + " para " + novoStatus
        );

        Long usuarioCriacao = jdbcTemplate.queryForObject("""
        select usuario_criacao
        from regulacao_tfd.solicitacoes
        where id = ?
        """, Long.class, id);

        String titulo = switch (novoStatus) {
            case "AUTORIZADA" -> "Solicitação aprovada";
            case "INDEFERIDA" -> "Solicitação reprovada";
            case "DEVOLVIDA_CORRECAO" -> "Solicitação devolvida para correção";
            default -> "Solicitação atualizada";
        };

        String mensagem = switch (novoStatus) {
            case "AUTORIZADA" -> "Sua solicitação foi aprovada pela regulação.";
            case "INDEFERIDA" -> "Sua solicitação foi reprovada pela regulação.";
            case "DEVOLVIDA_CORRECAO" -> "Sua solicitação foi devolvida para correção.";
            default -> "O status da sua solicitação foi atualizado para " + novoStatus + ".";
        };

        notificationService.notificarUsuario(usuarioCriacao, titulo, mensagem, "/solicitacoes/" + id);
    }

    public void registrarImpressao(Long id, Long usuarioId, String tipo, boolean reimpressao, String caminho, String hash) {
        jdbcTemplate.update("""
                insert into regulacao_tfd.impressoes
                (solicitacao_id, usuario_id, tipo_formulario, reimpressao, caminho_storage, hash_arquivo)
                values (?, ?, ?, ?, ?, ?)
                """, id, usuarioId, tipo, reimpressao, caminho, hash);
        jdbcTemplate.update("update regulacao_tfd.solicitacoes set status = 'IMPRESSA', data_impressao = current_timestamp where id = ?", id);
        historico(id, usuarioId, null, "IMPRESSA", reimpressao ? "REIMPRESSAO" : "IMPRESSAO", caminho);
    }

    private void historico(Long solicitacaoId, Long usuarioId, String anterior, String novo, String acao, String observacao) {
        jdbcTemplate.update("""
                insert into regulacao_tfd.solicitacao_historico
                (solicitacao_id, usuario_id, setor, status_anterior, status_novo, acao, observacao)
                values (?, ?, 'Regulacao TFD/APAC', ?, ?, ?, ?)
                """, solicitacaoId, usuarioId, anterior, novo, acao, observacao);
    }

    private String required(Map<String, String> form, String key) {
        String value = form.get(key);
        if (!StringUtils.hasText(value)) {
            throw new IllegalArgumentException("Campo obrigatorio: " + key);
        }
        return value;
    }

    private Long longValue(Map<String, String> form, String key) {
        return Long.valueOf(required(form, key));
    }

    private Long nullableLong(Map<String, String> form, String key) {
        String value = form.get(key);
        return StringUtils.hasText(value) ? Long.valueOf(value) : null;
    }

    private Integer intValue(Map<String, String> form, String key, int defaultValue) {
        String value = form.get(key);
        return StringUtils.hasText(value) ? Integer.valueOf(value) : defaultValue;
    }

    private Boolean booleanValue(Map<String, String> form, String key) {
        String value = form.get(key);
        return "true".equalsIgnoreCase(value) || "on".equalsIgnoreCase(value) || "sim".equalsIgnoreCase(value);
    }

    private Date date(String value, String label) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        try {
            return Date.valueOf(LocalDate.parse(value));
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException(label + " inválida. Use o seletor de data do formulário.");
        }
    }

    private String like(String value) {
        return "%" + nvl(value) + "%";
    }

    private String nvl(String value) {
        return value == null ? "" : value.trim();
    }

    private String procedimentoCodigoOuNull(Map<String, String> form) {
        String codigo = form.get("procedimento_codigo");
        if (StringUtils.hasText(codigo)) {
            return codigo.trim();
        }

        Long id = nullableLong(form, "procedimento_principal_id");
        if (id == null) {
            return null;
        }

        return jdbcTemplate.query("""
            select codigo
            from regulacao_tfd.procedimentos
            where id = ?
            limit 1
            """, rs -> rs.next() ? rs.getString("codigo") : null, id);
    }

    private Long procedimentoIdPorCodigoOuNull(Map<String, String> form) {
        Long id = nullableLong(form, "procedimento_principal_id");
        if (id != null) {
            return id;
        }

        String codigo = form.get("procedimento_codigo");
        if (!StringUtils.hasText(codigo)) {
            return null;
        }

        return jdbcTemplate.query("""
            select id
            from regulacao_tfd.procedimentos
            where codigo = ?
            limit 1
            """, rs -> rs.next() ? rs.getLong("id") : null, codigo);
    }

    private void validarTransicaoStatus(String atual, String novo) {
        if (!StringUtils.hasText(novo)) {
            throw new IllegalArgumentException("Novo status não informado.");
        }
        if (atual == null || atual.isBlank() || atual.equals(novo)) {
            return;
        }
        Map<String, Set<String>> transicoesPermitidas = Map.of(
                "ENVIADA", Set.of("EM_ANALISE", "DEVOLVIDA_CORRECAO", "AUTORIZADA", "INDEFERIDA"),
                "EM_ANALISE", Set.of("AUTORIZADA", "INDEFERIDA", "DEVOLVIDA_CORRECAO", "AGUARDANDO_DOCUMENTOS"),
                "AGUARDANDO_DOCUMENTOS", Set.of("EM_ANALISE", "DEVOLVIDA_CORRECAO", "AUTORIZADA", "INDEFERIDA"),
                "DEVOLVIDA_CORRECAO", Set.of("ENVIADA", "EM_ANALISE"),
                "AUTORIZADA", Set.of("IMPRESSA", "FINALIZADA"),
                "IMPRESSA", Set.of("FINALIZADA"),
                "INDEFERIDA", Set.of("FINALIZADA"),
                "CANCELADA", Set.of(),
                "FINALIZADA", Set.of()
        );
        Set<String> permitidos = transicoesPermitidas.getOrDefault(atual, Set.of());
        if (!permitidos.contains(novo)) {
            throw new IllegalStateException("Transição de status inválida: " + atual + " -> " + novo);
        }
    }
}
