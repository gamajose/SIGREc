package br.gov.sigrec.tfdapac.controller;

import br.gov.sigrec.tfdapac.service.CurrentUserService;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Map;

@Controller
public class PerfilController {
    private final JdbcTemplate jdbcTemplate;
    private final CurrentUserService currentUserService;
    private final PasswordEncoder passwordEncoder;

    public PerfilController(JdbcTemplate jdbcTemplate, CurrentUserService currentUserService, PasswordEncoder passwordEncoder) {
        this.jdbcTemplate = jdbcTemplate;
        this.currentUserService = currentUserService;
        this.passwordEncoder = passwordEncoder;
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/perfil")
    public String update(@RequestParam Map<String, String> form, Authentication auth, RedirectAttributes ra) {
        Long id = currentUserService.id(auth);
        String senha = form.get("senha");
        if (StringUtils.hasText(senha)) {
            String hash = passwordEncoder.encode(senha);
            jdbcTemplate.update("""
                    update regulacao_tfd.usuarios
                    set nome = ?, email = ?, endereco = ?, password_hash = ?, senha_hash = ?, updated_at = current_timestamp
                    where id = ?
                    """, form.get("nome"), form.get("email"), form.get("endereco"), hash, hash, id);
        } else {
            jdbcTemplate.update("""
                    update regulacao_tfd.usuarios
                    set nome = ?, email = ?, endereco = ?, updated_at = current_timestamp
                    where id = ?
                    """, form.get("nome"), form.get("email"), form.get("endereco"), id);
        }
        ra.addFlashAttribute("ok", "Perfil atualizado.");
        return "redirect:/";
    }
}
