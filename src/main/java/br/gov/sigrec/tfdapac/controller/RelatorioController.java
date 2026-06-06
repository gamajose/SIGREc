package br.gov.sigrec.tfdapac.controller;

import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import com.lowagie.text.Document;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;

import java.io.ByteArrayOutputStream;

@Controller
@PreAuthorize("hasRole('ADMIN')")
public class RelatorioController {
    private final JdbcTemplate jdbcTemplate;

    public RelatorioController(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @GetMapping("/relatorios")
    public String index(@RequestParam(defaultValue = "") String q,
                        @RequestParam(defaultValue = "") String dataInicio,
                        @RequestParam(defaultValue = "") String dataFim,
                        @RequestParam(defaultValue = "") String estado,
                        @RequestParam(defaultValue = "") String prioridade,
                        @RequestParam(defaultValue = "1") Integer page,
                        Model model) {
        int pageSize = 20;
        int paginaAtual = Math.max(page == null ? 1 : page, 1);
        int offset = (paginaAtual - 1) * pageSize;
        Integer total = jdbcTemplate.queryForObject("""
                select count(*)
                from regulacao_tfd.solicitacoes s
                join regulacao_tfd.pacientes p on p.id = s.paciente_id
                where s.status not in ('ENVIADA','EM_ANALISE')
                  and (? = '' or p.nome ilike ? or s.numero_protocolo ilike ?)
                  and (? = '' or s.status = ?)
                  and (? = '' or s.prioridade = ?)
                  and (? = '' or s.data_entrada::date >= ?::date)
                  and (? = '' or s.data_entrada::date <= ?::date)
                """, Integer.class,
                q, like(q), like(q), estado, estado, prioridade, prioridade, dataInicio, dataInicio, dataFim, dataFim);
        int totalRegistros = total == null ? 0 : total;
        int totalPaginas = Math.max((int) Math.ceil(totalRegistros / (double) pageSize), 1);
        if (paginaAtual > totalPaginas) paginaAtual = totalPaginas;
        offset = (paginaAtual - 1) * pageSize;

        model.addAttribute("q", q);
        model.addAttribute("dataInicio", dataInicio);
        model.addAttribute("dataFim", dataFim);
        model.addAttribute("estado", estado);
        model.addAttribute("prioridade", prioridade);
        model.addAttribute("page", paginaAtual);
        model.addAttribute("totalPaginas", totalPaginas);
        model.addAttribute("totalRegistros", totalRegistros);
        model.addAttribute("temAnterior", paginaAtual > 1);
        model.addAttribute("temProxima", paginaAtual < totalPaginas);
        model.addAttribute("estados", jdbcTemplate.queryForList("""
                select distinct status
                from regulacao_tfd.solicitacoes
                where status not in ('ENVIADA','EM_ANALISE')
                order by status
                """));
        model.addAttribute("prioridades", jdbcTemplate.queryForList("""
                select distinct prioridade
                from regulacao_tfd.solicitacoes
                where prioridade is not null
                order by prioridade
                """));
        model.addAttribute("historicoSolicitacoes", jdbcTemplate.queryForList("""
                select (? + row_number() over (order by coalesce(h.criado_em, s.data_analise, s.data_autorizacao, s.data_entrada) desc)) numero,
                       s.id,
                       s.numero_protocolo,
                       s.tipo_solicitacao,
                       p.nome paciente,
                       u.nome unidade,
                       coalesce(pr.codigo, s.procedimento_codigo_externo) procedimento_codigo,
                       coalesce(pr.descricao, vpu.descricao, '') procedimento_descricao,
                       s.status,
                       s.prioridade,
                       s.data_entrada,
                       s.data_analise,
                       s.data_autorizacao,
                       s.observacao_regulacao,
                       s.motivo_indeferimento,
                       h.observacao ultima_observacao,
                       h.criado_em ultima_movimentacao,
                       h.acao ultima_acao,
                       usu.nome usuario_movimentacao
                from regulacao_tfd.solicitacoes s
                join regulacao_tfd.pacientes p on p.id = s.paciente_id
                join regulacao_tfd.unidades_saude u on u.id = s.unidade_solicitante_id
                left join regulacao_tfd.procedimentos pr on pr.id = s.procedimento_principal_id
                left join regulacao_tfd.vw_procedimentos_unificados vpu on vpu.codigo = s.procedimento_codigo_externo
                left join lateral (
                    select h.*
                    from regulacao_tfd.solicitacao_historico h
                    where h.solicitacao_id = s.id
                    order by h.criado_em desc
                    limit 1
                ) h on true
                left join regulacao_tfd.usuarios usu on usu.id = h.usuario_id
                where s.status not in ('ENVIADA','EM_ANALISE')
                  and (? = '' or p.nome ilike ? or s.numero_protocolo ilike ?)
                  and (? = '' or s.status = ?)
                  and (? = '' or s.prioridade = ?)
                  and (? = '' or s.data_entrada::date >= ?::date)
                  and (? = '' or s.data_entrada::date <= ?::date)
                order by coalesce(h.criado_em, s.data_analise, s.data_autorizacao, s.data_entrada) desc
                limit ? offset ?
                """,
                offset,
                q, like(q), like(q),
                estado, estado,
                prioridade, prioridade,
                dataInicio, dataInicio,
                dataFim, dataFim,
                pageSize, offset));
        return "relatorios/index";
    }

    @GetMapping("/relatorios/fila.csv")
    public ResponseEntity<String> filaCsv() {
        StringBuilder csv = new StringBuilder("protocolo;tipo;paciente;unidade;procedimento;prioridade;status;data_entrada;data_decisao;motivo_parecer\n");
        jdbcTemplate.queryForList("""
                select s.numero_protocolo, s.tipo_solicitacao, p.nome paciente, u.nome unidade,
                       coalesce(pr.codigo || ' - ' || pr.descricao, coalesce(s.procedimento_codigo_externo, '')) procedimento,
                       s.prioridade, s.status, s.data_entrada,
                       coalesce(s.data_autorizacao, s.data_analise) data_decisao,
                       coalesce(s.motivo_indeferimento, s.observacao_regulacao, h.observacao, '') motivo_parecer
                from regulacao_tfd.solicitacoes s
                join regulacao_tfd.pacientes p on p.id = s.paciente_id
                join regulacao_tfd.unidades_saude u on u.id = s.unidade_solicitante_id
                left join regulacao_tfd.procedimentos pr on pr.id = s.procedimento_principal_id
                left join lateral (
                    select h.observacao
                    from regulacao_tfd.solicitacao_historico h
                    where h.solicitacao_id = s.id
                    order by h.criado_em desc
                    limit 1
                ) h on true
                where s.status not in ('ENVIADA','EM_ANALISE')
                order by coalesce(s.data_autorizacao, s.data_analise, s.data_entrada) desc
                """).forEach(r -> csv.append(escape(r.get("numero_protocolo"))).append(';')
                .append(escape(r.get("tipo_solicitacao"))).append(';')
                .append(escape(r.get("paciente"))).append(';')
                .append(escape(r.get("unidade"))).append(';')
                .append(escape(r.get("procedimento"))).append(';')
                .append(escape(r.get("prioridade"))).append(';')
                .append(escape(r.get("status"))).append(';')
                .append(escape(r.get("data_entrada"))).append(';')
                .append(escape(r.get("data_decisao"))).append(';')
                .append(escape(r.get("motivo_parecer"))).append('\n'));
        return ResponseEntity.ok()
                .contentType(new MediaType("text", "csv"))
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename("historico-solicitacoes.csv").build().toString())
                .body(csv.toString());
    }

    @GetMapping("/relatorios/fila.pdf")
    public ResponseEntity<byte[]> filaPdf() throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document document = new Document(PageSize.A4.rotate(), 24, 24, 24, 24);
        PdfWriter.getInstance(document, out);
        document.open();
        document.add(new Paragraph("Historico de solicitacoes TFD/APAC"));
        document.add(new Paragraph(" "));
        PdfPTable table = new PdfPTable(7);
        table.setWidthPercentage(100);
        table.addCell("Protocolo");
        table.addCell("Tipo");
        table.addCell("Paciente");
        table.addCell("Unidade");
        table.addCell("Status");
        table.addCell("Decisao");
        table.addCell("Motivo/Parecer");
        jdbcTemplate.queryForList("""
                select s.numero_protocolo, s.tipo_solicitacao, p.nome paciente, u.nome unidade,
                       s.status,
                       coalesce(s.data_autorizacao, s.data_analise) data_decisao,
                       coalesce(s.motivo_indeferimento, s.observacao_regulacao, h.observacao, '') motivo_parecer
                from regulacao_tfd.solicitacoes s
                join regulacao_tfd.pacientes p on p.id = s.paciente_id
                join regulacao_tfd.unidades_saude u on u.id = s.unidade_solicitante_id
                left join lateral (
                    select h.observacao
                    from regulacao_tfd.solicitacao_historico h
                    where h.solicitacao_id = s.id
                    order by h.criado_em desc
                    limit 1
                ) h on true
                where s.status not in ('ENVIADA','EM_ANALISE')
                order by coalesce(s.data_autorizacao, s.data_analise, s.data_entrada) desc
                """).forEach(r -> {
            table.addCell(escape(r.get("numero_protocolo")));
            table.addCell(escape(r.get("tipo_solicitacao")));
            table.addCell(escape(r.get("paciente")));
            table.addCell(escape(r.get("unidade")));
            table.addCell(escape(r.get("status")));
            table.addCell(escape(r.get("data_decisao")));
            table.addCell(escape(r.get("motivo_parecer")));
        });
        document.add(table);
        document.close();
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.inline().filename("historico-solicitacoes.pdf").build().toString())
                .body(out.toByteArray());
    }

    private String like(String value) {
        return "%" + (value == null ? "" : value.trim()) + "%";
    }

    private String escape(Object value) {
        return value == null ? "" : value.toString().replace(";", ",").replace("\n", " ");
    }
}
