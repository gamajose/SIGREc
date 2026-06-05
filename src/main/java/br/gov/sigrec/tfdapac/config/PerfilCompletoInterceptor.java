package br.gov.sigrec.tfdapac.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class PerfilCompletoInterceptor implements HandlerInterceptor {
    private final JdbcTemplate jdbcTemplate;

    public PerfilCompletoInterceptor(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String uri = request.getRequestURI();

        if (uri.equals("/login")
                || uri.equals("/logout")
                || uri.startsWith("/css/")
                || uri.startsWith("/js/")
                || uri.startsWith("/sounds/")
                || uri.startsWith("/api/notificacoes/")
                || uri.equals("/perfil/completo")) {
            return true;
        }

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getName())) {
            return true;
        }

        Boolean pendente = jdbcTemplate.queryForObject("""
                select coalesce(primeiro_login, true) or not coalesce(perfil_completo, false)
                from regulacao_tfd.usuarios
                where username = ?
                """, Boolean.class, authentication.getName());

        if (Boolean.TRUE.equals(pendente)) {
            response.sendRedirect("/perfil/completo");
            return false;
        }

        return true;
    }
}
