package br.gov.sigrec.tfdapac.service;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
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

    public Long unidadeId(Authentication authentication) {
        if (authentication == null) {
            return null;
        }
        return jdbcTemplate.queryForObject(
                "select unidade_id from regulacao_tfd.usuarios where username = ?",
                Long.class,
                authentication.getName());
    }

    public boolean isAdmin(Authentication authentication) {
        return hasRole(authentication, "ROLE_ADMIN");
    }

    public boolean isRegulador(Authentication authentication) {
        return hasRole(authentication, "ROLE_REGULADOR");
    }

    public boolean isSolicitante(Authentication authentication) {
        return hasRole(authentication, "ROLE_SOLICITANTE");
    }

    private boolean hasRole(Authentication authentication, String role) {
        if (authentication == null) {
            return false;
        }
        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(role::equals);
    }
}
