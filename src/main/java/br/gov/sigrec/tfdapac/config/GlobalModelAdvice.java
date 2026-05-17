package br.gov.sigrec.tfdapac.config;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import java.util.List;
import java.util.Map;

@ControllerAdvice
public class GlobalModelAdvice {
    private final JdbcTemplate jdbcTemplate;

    public GlobalModelAdvice(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @ModelAttribute("usuarioLogado")
    public String usuarioLogado(Authentication authentication) {
        return authentication == null ? "" : authentication.getName();
    }

    @ModelAttribute("adminLogado")
    public boolean adminLogado(Authentication authentication) {
        return authentication != null && authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch("ROLE_ADMIN"::equals);
    }

    @ModelAttribute("perfilLogado")
    public Map<String, Object> perfilLogado(Authentication authentication) {
        if (authentication == null) {
            return Map.of();
        }
        return jdbcTemplate.queryForMap("""
                select id, username, nome, email, endereco, role
                from regulacao_tfd.usuarios
                where username = ?
                """, authentication.getName());
    }

    @ModelAttribute("ticketsAbertos")
    public Integer ticketsAbertos(Authentication authentication) {
        if (authentication == null || !adminLogado(authentication)) {
            return 0;
        }
        return jdbcTemplate.queryForObject("select count(*) from regulacao_tfd.tickets_suporte where status = 'ABERTO'", Integer.class);
    }

    @ModelAttribute("statusSolicitacao")
    public List<String> statusSolicitacao() {
        return List.of(
                "ENVIADA",
                "EM_ANALISE",
                "DEVOLVIDA_CORRECAO",
                "AGUARDANDO_DOCUMENTOS",
                "AUTORIZADA",
                "INDEFERIDA",
                "IMPRESSA",
                "CANCELADA",
                "FINALIZADA"
        );
    }
}
