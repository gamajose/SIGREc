package br.gov.sigrec.tfdapac.service;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class LookupService {
    private final JdbcTemplate jdbcTemplate;

    public LookupService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<Map<String, Object>> pacientes(String termo) {
        String q = like(termo);
        return jdbcTemplate.queryForList("""
                select * from regulacao_tfd.pacientes
                where ? = '%%' or nome ilike ? or cns ilike ? or cpf ilike ?
                order by nome limit 100
                """, q, q, q, q);
    }

    public List<Map<String, Object>> unidades() {
        return jdbcTemplate.queryForList("select * from regulacao_tfd.unidades_saude order by nome");
    }

    public List<Map<String, Object>> unidadesAutorizadoras() {
        return jdbcTemplate.queryForList("""
                select * from regulacao_tfd.unidades_saude
                where ativo and tipo_unidade in ('AUTORIZADORA', 'AMBAS')
                order by nome
                """);
    }

    public List<Map<String, Object>> profissionais() {
        return jdbcTemplate.queryForList("select * from regulacao_tfd.profissionais where ativo order by nome");
    }

    public List<Map<String, Object>> autorizadores() {
        return jdbcTemplate.queryForList("select * from regulacao_tfd.profissionais where ativo and autorizador order by nome");
    }

    public List<Map<String, Object>> procedimentos(String termo) {
        String q = like(termo);
        return jdbcTemplate.queryForList("""
                select * from regulacao_tfd.procedimentos
                where ativo and (? = '%%' or codigo ilike ? or descricao ilike ?)
                order by descricao limit 100
                """, q, q, q);
    }

    public Map<String, Object> row(String table, Long id) {
        return jdbcTemplate.queryForMap("select * from regulacao_tfd." + table + " where id = ?", id);
    }

    private String like(String value) {
        return "%" + (value == null ? "" : value.trim()) + "%";
    }
}
