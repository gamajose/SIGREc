package br.gov.sigrec.tfdapac.config;

import br.gov.sigrec.tfdapac.service.AuditService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;

import java.util.List;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {
    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    UserDetailsService userDetailsService(JdbcTemplate jdbcTemplate) {
        return username -> {
            var users = jdbcTemplate.query("""
                    select id, username, senha_hash, ativo
                    from regulacao_tfd.usuarios
                    where username = ?
                    """, (rs, rowNum) -> new DbUser(
                    rs.getLong("id"),
                    rs.getString("username"),
                    rs.getString("senha_hash"),
                    rs.getBoolean("ativo")
            ), username);
            if (users.isEmpty()) {
                throw new UsernameNotFoundException("Usuario nao encontrado");
            }
            DbUser dbUser = users.getFirst();
            List<SimpleGrantedAuthority> authorities = jdbcTemplate.query("""
                    select r.nome
                    from regulacao_tfd.roles r
                    join regulacao_tfd.user_roles ur on ur.role_id = r.id
                    where ur.usuario_id = ?
                    """, (rs, rowNum) -> new SimpleGrantedAuthority(rs.getString("nome")), dbUser.id());
            return User.withUsername(dbUser.username())
                    .password(dbUser.password())
                    .disabled(!dbUser.active())
                    .authorities(authorities)
                    .build();
        };
    }

    @Bean
    AuthenticationManager authenticationManager(UserDetailsService userDetailsService, PasswordEncoder passwordEncoder) {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder);
        return new ProviderManager(provider);
    }

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http,
                                            AuditService auditService,
                                            JdbcTemplate jdbcTemplate) throws Exception {
        http
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/css/**", "/login").permitAll()
                        .requestMatchers("/usuarios/**", "/parametros/**").hasRole("ADMIN")
                        .requestMatchers("/regulacao/**").hasAnyRole("ADMIN", "REGULACAO", "AUTORIZADOR", "AUDITORIA")
                        .requestMatchers("/impressao/**").hasAnyRole("ADMIN", "REGULACAO", "AUTORIZADOR")
                        .anyRequest().authenticated()
                )
                .formLogin(form -> form
                        .loginPage("/login")
                        .successHandler(successHandler(auditService, jdbcTemplate))
                        .failureHandler(failureHandler(auditService))
                )
                .logout(logout -> logout.logoutSuccessUrl("/login?logout"))
                .headers(headers -> headers.frameOptions(frame -> frame.sameOrigin()));
        return http.build();
    }

    private AuthenticationSuccessHandler successHandler(AuditService auditService, JdbcTemplate jdbcTemplate) {
        return (request, response, authentication) -> {
            String username = authentication.getName();
            auditService.login(username, true, request);
            jdbcTemplate.update("update regulacao_tfd.usuarios set ultimo_login = current_timestamp where username = ?", username);
            response.sendRedirect("/");
        };
    }

    private AuthenticationFailureHandler failureHandler(AuditService auditService) {
        return (request, response, exception) -> {
            auditService.login(request.getParameter("username"), false, request);
            response.sendRedirect("/login?error");
        };
    }

    private record DbUser(Long id, String username, String password, boolean active) {
    }
}
