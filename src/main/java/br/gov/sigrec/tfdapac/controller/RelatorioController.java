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
    public String index(Model model) {
        model.addAttribute("porStatus", jdbcTemplate.queryForList("""
                select status, count(*) total from regulacao_tfd.solicitacoes group by status order by status
                """));
        model.addAttribute("porUnidade", jdbcTemplate.queryForList("""
                select u.nome unidade, count(*) total
                from regulacao_tfd.solicitacoes s join regulacao_tfd.unidades_saude u on u.id = s.unidade_solicitante_id
                group by u.nome order by u.nome
                """));
        model.addAttribute("tempoMedio", jdbcTemplate.queryForList("""
                select tipo_solicitacao, avg(extract(epoch from (data_autorizacao - data_entrada))/3600)::numeric(10,2) horas
                from regulacao_tfd.solicitacoes
                where data_autorizacao is not null
                group by tipo_solicitacao
                """));
        model.addAttribute("reimpressoes", jdbcTemplate.queryForList("""
                select s.numero_protocolo, p.nome paciente, i.tipo_formulario, i.criado_em, u.nome usuario
                from regulacao_tfd.impressoes i
                join regulacao_tfd.solicitacoes s on s.id = i.solicitacao_id
                join regulacao_tfd.pacientes p on p.id = s.paciente_id
                left join regulacao_tfd.usuarios u on u.id = i.usuario_id
                where i.reimpressao
                order by i.criado_em desc limit 50
                """));
        return "relatorios/index";
    }

    @GetMapping("/relatorios/fila.csv")
    public ResponseEntity<String> filaCsv() {
        StringBuilder csv = new StringBuilder("protocolo;tipo;paciente;unidade;procedimento;prioridade;status;data_entrada\n");
        jdbcTemplate.queryForList("""
                select s.numero_protocolo, s.tipo_solicitacao, p.nome paciente, u.nome unidade,
                       coalesce(pr.codigo || ' - ' || pr.descricao, '') procedimento,
                       s.prioridade, s.status, s.data_entrada
                from regulacao_tfd.solicitacoes s
                join regulacao_tfd.pacientes p on p.id = s.paciente_id
                join regulacao_tfd.unidades_saude u on u.id = s.unidade_solicitante_id
                left join regulacao_tfd.procedimentos pr on pr.id = s.procedimento_principal_id
                order by s.data_entrada desc
                """).forEach(r -> csv.append(escape(r.get("numero_protocolo"))).append(';')
                .append(escape(r.get("tipo_solicitacao"))).append(';')
                .append(escape(r.get("paciente"))).append(';')
                .append(escape(r.get("unidade"))).append(';')
                .append(escape(r.get("procedimento"))).append(';')
                .append(escape(r.get("prioridade"))).append(';')
                .append(escape(r.get("status"))).append(';')
                .append(escape(r.get("data_entrada"))).append('\n'));
        return ResponseEntity.ok()
                .contentType(new MediaType("text", "csv"))
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename("fila-tfd-apac.csv").build().toString())
                .body(csv.toString());
    }

    @GetMapping("/relatorios/fila.pdf")
    public ResponseEntity<byte[]> filaPdf() throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document document = new Document(PageSize.A4.rotate(), 24, 24, 24, 24);
        PdfWriter.getInstance(document, out);
        document.open();
        document.add(new Paragraph("Relatorio de fila TFD/APAC"));
        document.add(new Paragraph(" "));
        PdfPTable table = new PdfPTable(7);
        table.setWidthPercentage(100);
        table.addCell("Protocolo");
        table.addCell("Tipo");
        table.addCell("Paciente");
        table.addCell("Unidade solicitante");
        table.addCell("Unidade autorizadora");
        table.addCell("Prioridade");
        table.addCell("Status");
        jdbcTemplate.queryForList("""
                select s.numero_protocolo, s.tipo_solicitacao, p.nome paciente, u.nome unidade,
                       coalesce(ua.nome, '') autorizadora, s.prioridade, s.status
                from regulacao_tfd.solicitacoes s
                join regulacao_tfd.pacientes p on p.id = s.paciente_id
                join regulacao_tfd.unidades_saude u on u.id = s.unidade_solicitante_id
                left join regulacao_tfd.unidades_saude ua on ua.id = s.unidade_autorizadora_id
                order by s.data_entrada desc
                """).forEach(r -> {
            table.addCell(escape(r.get("numero_protocolo")));
            table.addCell(escape(r.get("tipo_solicitacao")));
            table.addCell(escape(r.get("paciente")));
            table.addCell(escape(r.get("unidade")));
            table.addCell(escape(r.get("autorizadora")));
            table.addCell(escape(r.get("prioridade")));
            table.addCell(escape(r.get("status")));
        });
        document.add(table);
        document.close();
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.inline().filename("fila-tfd-apac.pdf").build().toString())
                .body(out.toByteArray());
    }

    private String escape(Object value) {
        return value == null ? "" : value.toString().replace(";", ",").replace("\n", " ");
    }
}
