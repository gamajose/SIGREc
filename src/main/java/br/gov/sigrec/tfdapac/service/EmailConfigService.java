package br.gov.sigrec.tfdapac.service;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class EmailConfigService {

    private final JdbcTemplate jdbcTemplate;

    public EmailConfigService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Map<String, Object> buscar() {
        return jdbcTemplate.queryForMap("""
                select *
                from regulacao_tfd.configuracoes_email
                order by id
                limit 1
                """);
    }

    public void salvar(Map<String, String> form) {
        jdbcTemplate.update("""
                update regulacao_tfd.configuracoes_email
                set host = ?,
                    porta = ?,
                    usuario = ?,
                    senha = ?,
                    remetente = ?,
                    ativo = ?,
                    usar_tls = ?,
                    atualizado_em = current_timestamp
                where id = (
                    select id
                    from regulacao_tfd.configuracoes_email
                    order by id
                    limit 1
                )
                """,
                form.get("host"),
                inteiro(form.get("porta"), 587),
                form.get("usuario"),
                form.get("senha"),
                form.get("remetente"),
                "on".equals(form.get("ativo")),
                "on".equals(form.get("usar_tls")));
    }

    private Integer inteiro(String value, Integer padrao) {
        if (value == null || value.isBlank()) {
            return padrao;
        }

        try {
            return Integer.valueOf(value);
        } catch (Exception e) {
            return padrao;
        }
    }
}