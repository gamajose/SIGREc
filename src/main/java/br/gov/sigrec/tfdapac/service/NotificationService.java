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
                (usuario_id, titulo, mensagem, link, lida)
                values (?, ?, ?, ?, false)
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
        List<Long> usuarios = jdbcTemplate.queryForList("""
                select id
                from regulacao_tfd.usuarios
                where ativo = true
                  and role in ('ADMIN', 'REGULADOR')
                """, Long.class);

        for (Long usuarioId : usuarios) {
            notificarUsuario(usuarioId, titulo, mensagem, link);
        }
    }

    public List<Map<String, Object>> listarDoUsuario(Long usuarioId) {
        return jdbcTemplate.queryForList("""
                select *
                from regulacao_tfd.notificacoes
                where usuario_id = ?
                order by criado_em desc
                """, usuarioId);
    }

    public void marcarTodasComoLidas(Long usuarioId) {
        jdbcTemplate.update("""
                update regulacao_tfd.notificacoes
                set lida = true,
                    lida_em = current_timestamp
                where usuario_id = ?
                  and lida = false
                """, usuarioId);
    }

    public long contarNaoLidas(Long usuarioId) {
        Long total = jdbcTemplate.queryForObject("""
                select count(*)
                from regulacao_tfd.notificacoes
                where usuario_id = ?
                  and lida = false
                """, Long.class, usuarioId);

        return total == null ? 0L : total;
    }

    public void queueEmail(String para, String assunto, String mensagem) {
        emailService.enviar(para, assunto, mensagem);
    }
}
