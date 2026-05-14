package br.gov.sigrec.tfdapac.service;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.time.Year;

@Service
public class ProtocolService {
    private final JdbcTemplate jdbcTemplate;

    public ProtocolService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public String next(String tipo) {
        Long value = jdbcTemplate.queryForObject("select nextval('regulacao_tfd.solicitacoes_id_seq')", Long.class);
        return "%s-%s-%06d".formatted(tipo, Year.now().getValue(), value == null ? 1 : value);
    }
}
