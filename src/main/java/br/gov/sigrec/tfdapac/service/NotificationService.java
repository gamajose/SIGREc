package br.gov.sigrec.tfdapac.service;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class NotificationService {
    private final JdbcTemplate jdbcTemplate;

    public NotificationService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void queueEmail(String destino, String assunto, String corpo) {
        if (destino == null || destino.isBlank()) {
            return;
        }
        jdbcTemplate.update("""
                insert into regulacao_tfd.email_outbox (destino, assunto, corpo)
                values (?, ?, ?)
                """, destino, assunto, corpo);
    }
}
