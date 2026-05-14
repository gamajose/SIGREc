package br.gov.sigrec.tfdapac.service;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class AuditService {
    private final JdbcTemplate jdbcTemplate;

    public AuditService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void login(String username, boolean success, HttpServletRequest request) {
        jdbcTemplate.update("""
                insert into regulacao_tfd.auditoria_login (username, sucesso, ip, user_agent)
                values (?, ?, ?, ?)
                """,
                username == null ? "" : username,
                success,
                request.getRemoteAddr(),
                request.getHeader("User-Agent"));
    }
}
