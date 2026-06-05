package br.gov.sigrec.tfdapac.controller;

import br.gov.sigrec.tfdapac.service.CurrentUserService;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class NotificacaoController {

    private final JdbcTemplate jdbcTemplate;
    private final CurrentUserService currentUserService;

    public NotificacaoController(JdbcTemplate jdbcTemplate, CurrentUserService currentUserService) {
        this.jdbcTemplate = jdbcTemplate;
        this.currentUserService = currentUserService;
    }

    @GetMapping("/notificacoes")
    public String listar(Authentication auth, Model model) {
        Long usuarioId = currentUserService.id(auth);

        model.addAttribute("notificacoes", jdbcTemplate.queryForList("""
                select *
                from regulacao_tfd.notificacoes_sistema
                where usuario_id = ?
                order by criado_em desc
                limit 100
                """, usuarioId));

        return "notificacoes/list";
    }

    @GetMapping("/notificacoes/{id}/abrir")
    public String abrir(@PathVariable Long id, Authentication auth, RedirectAttributes ra) {
        Long usuarioId = currentUserService.id(auth);

        Map<String, Object> notificacao = jdbcTemplate.queryForMap("""
                select id, url
                from regulacao_tfd.notificacoes_sistema
                where id = ?
                  and usuario_id = ?
                """, id, usuarioId);

        jdbcTemplate.update("""
                update regulacao_tfd.notificacoes_sistema
                set lida = true,
                    lida_em = coalesce(lida_em, current_timestamp)
                where id = ?
                  and usuario_id = ?
                """, id, usuarioId);

        Object url = notificacao.get("url");
        String destino = url == null ? "" : url.toString().trim();
        if (destino.isBlank()) {
            ra.addFlashAttribute("ok", "Notificação marcada como lida.");
            return "redirect:/notificacoes";
        }

        if (!destino.startsWith("/")) {
            destino = "/" + destino;
        }

        return "redirect:" + destino;
    }

    @GetMapping("/api/notificacoes/contador")
    @ResponseBody
    public Map<String, Object> contador(Authentication auth) {
        Long usuarioId = currentUserService.id(auth);

        Integer total = jdbcTemplate.queryForObject("""
                select count(*)
                from regulacao_tfd.notificacoes_sistema
                where usuario_id = ?
                  and lida = false
                """, Integer.class, usuarioId);

        return Map.of("naoLidas", total == null ? 0 : total);
    }

    @PostMapping("/notificacoes/{id}/ler")
    public String marcarComoLida(@PathVariable Long id, Authentication auth) {
        Long usuarioId = currentUserService.id(auth);

        jdbcTemplate.update("""
                update regulacao_tfd.notificacoes_sistema
                set lida = true,
                    lida_em = current_timestamp
                where id = ?
                  and usuario_id = ?
                """, id, usuarioId);

        return "redirect:/notificacoes";
    }

    @PostMapping("/notificacoes/ler-todas")
    public String marcarTodasComoLidas(Authentication auth) {
        Long usuarioId = currentUserService.id(auth);

        jdbcTemplate.update("""
                update regulacao_tfd.notificacoes_sistema
                set lida = true,
                    lida_em = current_timestamp
                where usuario_id = ?
                  and lida = false
                """, usuarioId);

        return "redirect:/notificacoes";
    }
}
