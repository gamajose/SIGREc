package br.gov.sigrec.tfdapac.service;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class NotificationService {

    private final JdbcTemplate jdbcTemplate;
    private final EmailService emailService;

    public NotificationService(JdbcTemplate jdbcTemplate, EmailService emailService) {
        this.jdbcTemplate = jdbcTemplate;
        this.emailService = emailService;
    }

    public void notificarUsuario(Long usuarioId, String titulo, String mensagem, String link) {
        if (usuarioId == null) {
            return;
        }

        jdbcTemplate.update("""
                insert into regulacao_tfd.notificacoes
                (usuario_id, titulo, mensagem, link)
                values (?, ?, ?, ?)
                """, usuarioId, titulo, mensagem, link);

        Map<String, Object> usuario = jdbcTemplate.queryForMap("""
                select email
                from regulacao_tfd.usuarios
                where id = ?
                """, usuarioId);

        Object email = usuario.get("email");
        if (email != null) {
            emailService.enviar(
                    email.toString(),
                    titulo,
                    mensagem + "\n\nAcesse: " + link
            );
        }
    }

    public void notificarReguladores(String titulo, String mensagem, String link) {
        List<Long> reguladores = jdbcTemplate.queryForList("""
                select id
                from regulacao_tfd.usuarios
                where ativo = true
                  and role = 'REGULADOR'
                """, Long.class);

        for (Long usuarioId : reguladores) {
            notificarUsuario(usuarioId, titulo, mensagem, link);
        }
    }
}