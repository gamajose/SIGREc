package br.gov.sigrec.tfdapac.config;

import br.gov.sigrec.tfdapac.service.AuditService;
import br.gov.sigrec.tfdapac.repository.UserAccountRepository;
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
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
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
    UserDetailsService userDetailsService(UserAccountRepository userAccountRepository) {
        return username -> {
            var dbUser = userAccountRepository.findByUsername(username)
                    .orElseThrow(() -> new UsernameNotFoundException("Usuario nao encontrado"));
            List<SimpleGrantedAuthority> authorities = List.of(new SimpleGrantedAuthority("ROLE_" + dbUser.getRole().name()));
            return User.withUsername(dbUser.getUsername())
                    .password(dbUser.getPasswordHash())
                    .disabled(!Boolean.TRUE.equals(dbUser.getAtivo()))
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
                                            JdbcTemplate jdbcTemplate,
                                            JwtAuthenticationFilter jwtAuthenticationFilter) throws Exception {
        http
                .csrf(csrf -> csrf.ignoringRequestMatchers("/api/**", "/requisicoes/**"))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/auth/token").permitAll()
                        .requestMatchers("/css/**", "/sounds/**", "/login").permitAll()
                        .requestMatchers("/requisicoes/**").hasAnyRole("ADMIN", "SOLICITANTE")
                        .requestMatchers("/usuarios/**", "/parametros/**").hasRole("ADMIN")
                        .requestMatchers("/regulacao/**").hasAnyRole("ADMIN", "REGULADOR")
                        .requestMatchers("/relatorios/**").hasRole("ADMIN")
                        .requestMatchers("/solicitacoes/**").hasAnyRole("ADMIN", "SOLICITANTE", "REGULADOR")
                        .requestMatchers("/impressao/**").permitAll()
                        .requestMatchers("/pacientes/**", "/unidades/**", "/profissionais/**", "/procedimentos/**", "/anexos/**").hasAnyRole("ADMIN", "SOLICITANTE", "REGULADOR")
                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
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
}
