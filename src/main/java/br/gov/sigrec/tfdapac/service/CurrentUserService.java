package br.gov.sigrec.tfdapac.service;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

@Service
public class CurrentUserService {
    private final JdbcTemplate jdbcTemplate;

    public CurrentUserService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Long id(Authentication authentication) {
        if (authentication == null) {
            return null;
        }
        return jdbcTemplate.queryForObject(
                "select id from regulacao_tfd.usuarios where username = ?",
                Long.class,
                authentication.getName());
    }
}
