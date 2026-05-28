package br.gov.sigrec.tfdapac.service;

import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.Map;

@Service
public class PdfService {
    private final Path storagePath;

    public PdfService(@Value("${sigrec.storage-path}") String storagePath) {
        this.storagePath = Path.of(storagePath);
    }

    public GeneratedPdf gerar(Map<String, Object> solicitacao, Map<String, Object> complemento) {
        try {
            byte[] bytes = gerarBytes(solicitacao, complemento);
            String hash = sha256(bytes);
            return new GeneratedPdf(bytes, null, hash);
        } catch (Exception e) {
            throw new IllegalStateException("Falha ao gerar PDF", e);
        }
    }

    public GeneratedPdf gerarESalvar(Map<String, Object> solicitacao, Map<String, Object> complemento) {
        try {
            byte[] bytes = gerarBytes(solicitacao, complemento);
            String hash = sha256(bytes);
            String tipo = text(solicitacao, "tipo_solicitacao");
            Path dir = storagePath.resolve("impressoes").resolve(text(solicitacao, "numero_protocolo"));
            Files.createDirectories(dir);
            Path file = dir.resolve(tipo + "-" + System.currentTimeMillis() + ".pdf");
            Files.write(file, bytes);
            return new GeneratedPdf(bytes, file.toAbsolutePath().toString(), hash);
        } catch (Exception e) {
            throw new IllegalStateException("Falha ao gerar PDF", e);
        }
    }

    private byte[] gerarBytes(Map<String, Object> solicitacao, Map<String, Object> complemento) throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document doc = new Document(PageSize.A4, 22, 22, 18, 18);
        PdfWriter.getInstance(doc, out);
        doc.open();
        String tipo = text(solicitacao, "tipo_solicitacao");
        if ("APAC".equals(tipo)) {
            apac(doc, solicitacao, complemento);
        } else {
            tfd(doc, solicitacao, complemento);
        }
        doc.close();
        return out.toByteArray();
    }

    private void tfd(Document doc, Map<String, Object> s, Map<String, Object> tfd) throws Exception {
        title(doc, "LAUDO PARA CADASTRO DE PACIENTE - TFD");
        alert(doc, "TODO LAUDO DEVERA SER PREENCHIDO PELO MEDICO");
        table(doc, "Identificacao do estabelecimento solicitante",
                row("CNES", s.get("unidade_cnes"), "Estabelecimento", s.get("unidade_nome")),
                row("Municipio", s.get("unidade_municipio"), "UF", s.get("unidade_uf")));
        table(doc, "Dados do paciente",
                row("Cartao SUS", s.get("paciente_cns"), "Nome", s.get("paciente_nome")),
                row("Nascimento", s.get("paciente_nascimento"), "CPF", s.get("paciente_cpf")),
                row("RG", s.get("paciente_rg"), "Nome da mae", s.get("paciente_mae")),
                row("Endereco", s.get("paciente_endereco"), "Municipio", s.get("paciente_municipio")),
                row("CEP/UF", text(s, "paciente_cep") + " " + text(s, "paciente_uf"), "Telefone", s.get("paciente_telefone")),
                row("E-mail", s.get("paciente_email"), "Protocolo", s.get("numero_protocolo")));
        table(doc, "Informacoes do acompanhante",
                row("Necessita acompanhante", truth(tfd.get("necessita_acompanhante")), "Nome", tfd.get("acompanhante_nome")),
                row("Nascimento", tfd.get("acompanhante_data_nascimento"), "CPF", tfd.get("acompanhante_cpf")),
                row("Telefone", tfd.get("acompanhante_telefone"), "Destino", tfd.get("destino")));
        block(doc, "CID-10 principal", text(s, "cid10_principal") + " - " + text(s, "descricao_diagnostico"));
        block(doc, "Tratamentos previos", text(tfd, "tratamentos_previos"));
        block(doc, "Procedimento/exame indicado para TFD", text(tfd, "procedimento_exame_indicado"));
        block(doc, "Principais sinais e sintomas clinicos", text(tfd, "sinais_sintomas"));
        block(doc, "Justificativa medica", text(tfd, "justificativa_medica"));
        signature(doc, s);
    }

    private void apac(Document doc, Map<String, Object> s, Map<String, Object> apac) throws Exception {
        title(doc, "LAUDO PARA SOLICITACAO/AUTORIZACAO DE APAC");
        table(doc, "Estabelecimento solicitante",
                row("Nome", s.get("unidade_nome"), "CNES", s.get("unidade_cnes")));
        table(doc, "Paciente",
                row("Nome", s.get("paciente_nome"), "Sexo", s.get("paciente_sexo")),
                row("Prontuario", apac.get("prontuario"), "CNS", s.get("paciente_cns")),
                row("Nascimento", s.get("paciente_nascimento"), "Raca/cor", apac.get("raca_cor")),
                row("Mae", s.get("paciente_mae"), "Telefone", s.get("paciente_telefone")),
                row("Responsavel", apac.get("responsavel"), "Municipio/IBGE", text(s, "paciente_municipio") + " / " + text(apac, "ibge")),
                row("Endereco", s.get("paciente_endereco"), "CEP/UF", text(s, "paciente_cep") + " " + text(s, "paciente_uf")));
        table(doc, "Procedimentos",
                row("Principal", text(s, "procedimento_codigo") + " - " + text(s, "procedimento_descricao"), "Qtd", apac.get("quantidade")),
                row("CID principal", s.get("cid10_principal"), "CID secundario", s.get("cid10_secundario")),
                row("Executante", apac.get("estabelecimento_executante"), "CNES", apac.get("cnes_executante")),
                row("Numero APAC", apac.get("numero_apac"), "Validade", text(apac, "validade_inicio") + " a " + text(apac, "validade_fim")));
        block(doc, "Diagnostico", text(s, "descricao_diagnostico"));
        block(doc, "Causas associadas", text(apac, "causas_associadas"));
        block(doc, "Observacoes", text(apac, "observacoes"));
        signature(doc, s);
    }

    private void title(Document doc, String text) throws Exception {
        Font font = new Font(Font.HELVETICA, 12, Font.BOLD);
        Paragraph p = new Paragraph(text, font);
        p.setAlignment(Element.ALIGN_CENTER);
        p.setSpacingAfter(5);
        doc.add(p);
    }

    private void alert(Document doc, String text) throws Exception {
        Font font = new Font(Font.HELVETICA, 11, Font.BOLD);
        Paragraph p = new Paragraph(text, font);
        p.setAlignment(Element.ALIGN_CENTER);
        p.setSpacingAfter(4);
        doc.add(p);
    }

    private void table(Document doc, String title, String[]... rows) throws Exception {
        blockTitle(doc, title);
        PdfPTable table = new PdfPTable(4);
        table.setWidthPercentage(100);
        table.setSpacingAfter(4);
        for (String[] row : rows) {
            cell(table, row[0], true);
            cell(table, row[1], false);
            cell(table, row[2], true);
            cell(table, row[3], false);
        }
        doc.add(table);
    }

    private void block(Document doc, String title, String value) throws Exception {
        blockTitle(doc, title);
        PdfPTable table = new PdfPTable(1);
        table.setWidthPercentage(100);
        Font font = new Font(Font.HELVETICA, 10, Font.NORMAL);
        PdfPCell cell = new PdfPCell(new Phrase(value == null ? "" : value, font));
        cell.setMinimumHeight(34);
        cell.setPadding(4);
        table.addCell(cell);
        table.setSpacingAfter(4);
        doc.add(table);
    }

    private void signature(Document doc, Map<String, Object> s) throws Exception {
        table(doc, "Profissionais",
                row("Solicitante", s.get("profissional_solicitante_nome"), "Documento", s.get("profissional_solicitante_doc")),
                row("Conselho/registro", text(s, "profissional_solicitante_conselho") + " " + text(s, "profissional_solicitante_registro"),
                        "Autorizador", s.get("profissional_autorizador_nome")));
    }

    private void blockTitle(Document doc, String title) throws Exception {
        Font font = new Font(Font.HELVETICA, 11, Font.BOLD);
        Paragraph p = new Paragraph(title.toUpperCase(), font);
        p.setSpacingBefore(2);
        p.setSpacingAfter(1);
        doc.add(p);
    }

    private void cell(PdfPTable table, Object value, boolean label) {
        Font font = new Font(Font.HELVETICA, 10, label ? Font.BOLD : Font.NORMAL);
        PdfPCell cell = new PdfPCell(new Phrase(value == null ? "" : value.toString(), font));
        cell.setPadding(3);
        cell.setBorder(Rectangle.BOX);
        table.addCell(cell);
    }

    private String[] row(String a, Object b, String c, Object d) {
        return new String[]{a, b == null ? "" : b.toString(), c, d == null ? "" : d.toString()};
    }

    private String truth(Object value) {
        return Boolean.TRUE.equals(value) ? "Sim" : "Nao";
    }

    private String text(Map<String, Object> map, String key) {
        Object value = map.get(key);
        return value == null ? "" : value.toString();
    }

    private String sha256(byte[] bytes) throws IOException {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
        } catch (Exception e) {
            throw new IOException(e);
        }
    }

    public record GeneratedPdf(byte[] bytes, String path, String hash) {
    }
}
