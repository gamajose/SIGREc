package br.gov.sigrec.tfdapac.service;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Map;
import java.util.Properties;

@Service
public class EmailService {

    private final JdbcTemplate jdbcTemplate;

    public EmailService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void enviar(String para, String assunto, String mensagem) {
        if (!StringUtils.hasText(para)) {
            return;
        }

        Map<String, Object> cfg = jdbcTemplate.queryForMap("""
                select *
                from regulacao_tfd.configuracoes_email
                order by id
                limit 1
                """);

        Boolean ativo = (Boolean) cfg.get("ativo");
        if (!Boolean.TRUE.equals(ativo)) {
            return;
        }

        String host = texto(cfg.get("host"));
        String usuario = texto(cfg.get("usuario"));
        String senha = texto(cfg.get("senha"));
        String remetente = texto(cfg.get("remetente"));
        Integer porta = ((Number) cfg.get("porta")).intValue();
        Boolean usarTls = (Boolean) cfg.get("usar_tls");

        if (!StringUtils.hasText(host) || !StringUtils.hasText(remetente)) {
            return;
        }

        JavaMailSenderImpl sender = new JavaMailSenderImpl();
        sender.setHost(host);
        sender.setPort(porta);
        sender.setUsername(usuario);
        sender.setPassword(senha);

        Properties props = sender.getJavaMailProperties();
        props.put("mail.smtp.auth", StringUtils.hasText(usuario) ? "true" : "false");
        props.put("mail.smtp.starttls.enable", Boolean.TRUE.equals(usarTls) ? "true" : "false");

        SimpleMailMessage mail = new SimpleMailMessage();
        mail.setFrom(remetente);
        mail.setTo(para);
        mail.setSubject(assunto);
        mail.setText(mensagem);

        try {
            sender.send(mail);
        } catch (Exception e) {
            System.err.println("Falha ao enviar e-mail: " + e.getMessage());
        }
    }

    private String texto(Object value) {
        return value == null ? "" : value.toString();
    }
}
