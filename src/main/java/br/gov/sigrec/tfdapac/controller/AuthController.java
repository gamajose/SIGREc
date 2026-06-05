package br.gov.sigrec.tfdapac.controller;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class AuthController {
    private final JdbcTemplate jdbcTemplate;

    public AuthController(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @GetMapping("/login")
    public String login() {
        return "auth/login";
    }

    @GetMapping("/esqueci-senha")
    public String esqueciSenha(Model model) {
        return "auth/esqueci-senha";
    }

    @PostMapping("/esqueci-senha")
    public String solicitarRecuperacao(@RequestParam String identificador, RedirectAttributes ra) {
        Integer total = jdbcTemplate.queryForObject("""
                select count(*)
                from regulacao_tfd.usuarios
                where lower(username) = lower(?)
                   or lower(coalesce(codigo_usuario, '')) = lower(?)
                   or lower(coalesce(email, '')) = lower(?)
                """, Integer.class, identificador, identificador, identificador);

        if (total != null && total > 0) {
            ra.addFlashAttribute("ok", "Solicitacao registrada. Procure o administrador do sistema para redefinir sua senha.");
        } else {
            ra.addFlashAttribute("ok", "Se os dados informados existirem, a solicitacao sera encaminhada ao administrador.");
        }

        return "redirect:/esqueci-senha";
    }
}
