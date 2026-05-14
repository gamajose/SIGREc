package br.gov.sigrec.tfdapac.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class AdminPasswordInitializer {
    @Bean
    CommandLineRunner ensureAdminPassword(JdbcTemplate jdbcTemplate, PasswordEncoder passwordEncoder) {
        return args -> {
            String hash = passwordEncoder.encode("admin123");
            jdbcTemplate.update("""
                    update regulacao_tfd.usuarios
                    set senha_hash = ?
                    where username = 'admin'
                      and senha_hash = '$2a$10$8c0lZA6Wv7dP1eMyd1sOjuowDmhRcqXyd7gYM8AIkWIXUkDkcc5v2'
                    """, hash);
        };
    }
}
