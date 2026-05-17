package br.gov.sigrec.tfdapac.controller;

import br.gov.sigrec.tfdapac.service.CurrentUserService;
import br.gov.sigrec.tfdapac.service.NotificationService;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class TicketController {
    private final JdbcTemplate jdbcTemplate;
    private final CurrentUserService currentUserService;
    private final NotificationService notificationService;

    public TicketController(JdbcTemplate jdbcTemplate, CurrentUserService currentUserService, NotificationService notificationService) {
        this.jdbcTemplate = jdbcTemplate;
        this.currentUserService = currentUserService;
        this.notificationService = notificationService;
    }

    @GetMapping("/suporte")
    public String suporte(Model model, Authentication auth) {
        Long usuarioId = currentUserService.id(auth);
        model.addAttribute("tickets", jdbcTemplate.queryForList("""
                select * from regulacao_tfd.tickets_suporte
                where usuario_id = ?
                order by criado_em desc
                """, usuarioId));
        return "tickets/suporte";
    }

    @PostMapping("/suporte")
    public String abrir(@RequestParam String assunto,
                        @RequestParam String mensagem,
                        Authentication auth,
                        RedirectAttributes ra) {
        Long usuarioId = currentUserService.id(auth);
        String email = jdbcTemplate.queryForObject("select email from regulacao_tfd.usuarios where id = ?", String.class, usuarioId);
        jdbcTemplate.update("""
                insert into regulacao_tfd.tickets_suporte (usuario_id, assunto, mensagem, email_destino)
                values (?, ?, ?, ?)
                """, usuarioId, assunto, mensagem, email);
        ra.addFlashAttribute("ok", "Solicitacao enviada para o suporte.");
        return "redirect:/suporte";
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/tickets")
    public String tickets(Model model) {
        model.addAttribute("tickets", jdbcTemplate.queryForList("""
                select t.*, u.username, u.nome, u.email
                from regulacao_tfd.tickets_suporte t
                join regulacao_tfd.usuarios u on u.id = t.usuario_id
                order by case t.status when 'ABERTO' then 1 else 2 end, t.criado_em desc
                """));
        return "tickets/list";
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/tickets/{id}/responder")
    public String responder(@PathVariable Long id,
                            @RequestParam String resposta,
                            Authentication auth,
                            RedirectAttributes ra) {
        Long adminId = currentUserService.id(auth);
        var ticket = jdbcTemplate.queryForMap("""
                select t.*, u.email, u.username
                from regulacao_tfd.tickets_suporte t
                join regulacao_tfd.usuarios u on u.id = t.usuario_id
                where t.id = ?
                """, id);
        jdbcTemplate.update("""
                update regulacao_tfd.tickets_suporte
                set status = 'RESOLVIDO', resposta = ?, respondido_por = ?, respondido_em = current_timestamp
                where id = ?
                """, resposta, adminId, id);
        notificationService.queueEmail(
                ticket.get("email") == null ? null : ticket.get("email").toString(),
                "SIGREc - Ticket resolvido #" + id,
                "Seu ticket foi resolvido.\n\nAssunto: " + ticket.get("assunto") + "\nResposta: " + resposta);
        ra.addFlashAttribute("ok", "Ticket respondido. E-mail colocado na fila de envio.");
        return "redirect:/tickets";
    }
}
