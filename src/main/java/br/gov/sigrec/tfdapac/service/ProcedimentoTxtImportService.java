package br.gov.sigrec.tfdapac.service;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;

@Service
public class ProcedimentoTxtImportService {
    private final JdbcTemplate jdbcTemplate;

    public ProcedimentoTxtImportService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public int importar0202(Path arquivo) {
        if (!Files.exists(arquivo)) {
            throw new IllegalArgumentException("Arquivo nao encontrado: " + arquivo.toAbsolutePath());
        }
        int count = 0;
        try (var lines = Files.lines(arquivo, Charset.forName("windows-1252"))) {
            for (String line : lines.filter(l -> l.startsWith("0202")).toList()) {
                String codigo = line.substring(0, 10).trim();
                String descricao = safeSubstring(line, 10, 250).trim().replaceAll("\\s+", " ");
                BigDecimal valor = parseValor(line);
                jdbcTemplate.update("""
                        insert into regulacao_tfd.procedimentos (codigo, descricao, tipo, valor, origem, ativo)
                        values (?, ?, 'EXAME_SANGUE', ?, 'tb_procedimento', true)
                        on conflict (codigo) do update
                        set descricao = excluded.descricao,
                            tipo = excluded.tipo,
                            valor = excluded.valor,
                            origem = excluded.origem,
                            ativo = true
                        """, codigo, descricao, valor);
                count++;
            }
        } catch (Exception e) {
            throw new IllegalStateException("Falha ao importar tb_procedimento", e);
        }
        return count;
    }

    private BigDecimal parseValor(String line) {
        String raw = line.length() >= 308 ? line.substring(302, 308) : "000000";
        return new BigDecimal(raw).movePointLeft(2);
    }

    private String safeSubstring(String value, int start, int length) {
        if (value.length() <= start) {
            return "";
        }
        int end = Math.min(value.length(), start + length);
        return value.substring(start, end);
    }
}
