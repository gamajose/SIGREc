package br.gov.sigrec.tfdapac.service;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.sql.Date;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Service
public class SolicitacaoService {
    private final JdbcTemplate jdbcTemplate;
    private final ProtocolService protocolService;

    public SolicitacaoService(JdbcTemplate jdbcTemplate, ProtocolService protocolService) {
        this.jdbcTemplate = jdbcTemplate;
        this.protocolService = protocolService;
    }

    public List<Map<String, Object>> fila(String status, String tipo, String termo) {
        return jdbcTemplate.queryForList("""
                select s.*, p.nome paciente_nome, p.cns paciente_cns, u.nome unidade_nome, pr.codigo procedimento_codigo, pr.descricao procedimento_descricao
                from regulacao_tfd.solicitacoes s
                join regulacao_tfd.pacientes p on p.id = s.paciente_id
                join regulacao_tfd.unidades_saude u on u.id = s.unidade_solicitante_id
                left join regulacao_tfd.procedimentos pr on pr.id = s.procedimento_principal_id
                where (? = '' or s.status = ?)
                  and (? = '' or s.tipo_solicitacao = ?)
                  and (? = '' or p.nome ilike ? or s.numero_protocolo ilike ? or coalesce(pr.descricao,'') ilike ?)
                order by
                  case s.prioridade when 'URGENTE' then 1 when 'ALTA' then 2 when 'NORMAL' then 3 else 4 end,
                  s.data_entrada
                """, nvl(status), nvl(status), nvl(tipo), nvl(tipo), nvl(termo), like(termo), like(termo), like(termo));
    }

    public Map<String, Object> detalhe(Long id) {
        return jdbcTemplate.queryForMap("""
                select s.*, p.nome paciente_nome, p.cns paciente_cns, p.cpf paciente_cpf, p.rg paciente_rg,
                       p.data_nascimento paciente_nascimento, p.sexo paciente_sexo, p.nome_mae paciente_mae,
                       p.endereco paciente_endereco, p.municipio paciente_municipio, p.uf paciente_uf, p.cep paciente_cep,
                       p.telefone paciente_telefone, p.email paciente_email,
                       u.nome unidade_nome, u.cnes unidade_cnes, u.municipio unidade_municipio, u.uf unidade_uf,
                       ps.nome profissional_solicitante_nome, ps.cpf_cns profissional_solicitante_doc,
                       ps.conselho profissional_solicitante_conselho, ps.registro_conselho profissional_solicitante_registro,
                       pa.nome profissional_autorizador_nome,
                       pr.codigo procedimento_codigo, pr.descricao procedimento_descricao
                from regulacao_tfd.solicitacoes s
                join regulacao_tfd.pacientes p on p.id = s.paciente_id
                join regulacao_tfd.unidades_saude u on u.id = s.unidade_solicitante_id
                left join regulacao_tfd.profissionais ps on ps.id = s.profissional_solicitante_id
                left join regulacao_tfd.profissionais pa on pa.id = s.profissional_autorizador_id
                left join regulacao_tfd.procedimentos pr on pr.id = s.procedimento_principal_id
                where s.id = ?
                """, id);
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
                (numero_protocolo, tipo_solicitacao, paciente_id, unidade_solicitante_id, profissional_solicitante_id,
                 procedimento_principal_id, cid10_principal, cid10_secundario, descricao_diagnostico, justificativa,
                 prioridade, status, data_envio, usuario_criacao, usuario_ultima_alteracao)
                values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 'ENVIADA', current_timestamp, ?, ?)
                returning id
                """, Long.class,
                protocolo,
                tipo,
                longValue(form, "paciente_id"),
                longValue(form, "unidade_solicitante_id"),
                nullableLong(form, "profissional_solicitante_id"),
                nullableLong(form, "procedimento_principal_id"),
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
                    form.get("acompanhante_nome"), date(form.get("acompanhante_data_nascimento")),
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
                    form.get("observacoes"), date(form.get("data_solicitacao")),
                    form.get("numero_apac"), date(form.get("validade_inicio")), date(form.get("validade_fim")),
                    form.get("estabelecimento_executante"), form.get("cnes_executante"));
        }
        historico(id, usuarioId, null, "ENVIADA", "CRIACAO", "Solicitacao enviada para regulacao");
        return id;
    }

    @Transactional
    public void mudarStatus(Long id, String novoStatus, String observacao, Long usuarioId, Map<String, String> form) {
        String anterior = jdbcTemplate.queryForObject("select status from regulacao_tfd.solicitacoes where id = ?", String.class, id);
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
                    date(form.get("validade_inicio")), date(form.get("validade_fim")),
                    nullableLong(form, "profissional_autorizador_id"), usuarioId);
        }
        historico(id, usuarioId, anterior, novoStatus, "MUDANCA_STATUS", observacao);
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

    private Date date(String value) {
        return StringUtils.hasText(value) ? Date.valueOf(LocalDate.parse(value)) : null;
    }

    private String like(String value) {
        return "%" + nvl(value) + "%";
    }

    private String nvl(String value) {
        return value == null ? "" : value.trim();
    }
}
