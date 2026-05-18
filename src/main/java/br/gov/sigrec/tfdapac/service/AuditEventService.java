package br.gov.sigrec.tfdapac.service;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class AuditEventService {

    private final JdbcTemplate jdbcTemplate;

    public AuditEventService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void registrar(Long usuarioId,
                          String entidade,
                          Long entidadeId,
                          String acao,
                          String detalhes) {
        jdbcTemplate.update("""
                insert into regulacao_tfd.auditoria_eventos
                (usuario_id, entidade, entidade_id, acao, detalhes)
                values (?, ?, ?, ?, ?)
                """, usuarioId, entidade, entidadeId, acao, detalhes);
    }
}