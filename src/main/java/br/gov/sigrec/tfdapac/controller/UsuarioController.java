package br.gov.sigrec.tfdapac.controller;

import br.gov.sigrec.tfdapac.domain.UserRole;
import br.gov.sigrec.tfdapac.service.CurrentUserService;
import br.gov.sigrec.tfdapac.service.UserAccountService;
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

import java.util.Map;

@Controller
@PreAuthorize("hasRole('ADMIN')")
public class UsuarioController {

    private final JdbcTemplate jdbcTemplate;
    private final UserAccountService userAccountService;
    private final CurrentUserService currentUserService;

    public UsuarioController(JdbcTemplate jdbcTemplate, UserAccountService userAccountService, CurrentUserService currentUserService) {
        this.jdbcTemplate = jdbcTemplate;
        this.userAccountService = userAccountService;
        this.currentUserService = currentUserService;
    }

    @GetMapping("/usuarios")
    public String list(Model model) {
        model.addAttribute("usuarios", jdbcTemplate.queryForList("""
                select u.*, us.nome unidade_nome
                from regulacao_tfd.usuarios u
                left join regulacao_tfd.unidades_saude us on us.id = u.unidade_id
                order by u.username
                """));
        model.addAttribute("roles", UserRole.values());
        model.addAttribute("unidades", jdbcTemplate.queryForList("select id, nome from regulacao_tfd.unidades_saude order by nome"));
        return "usuarios/list";
    }

    @PostMapping("/usuarios")
    public String criar(@RequestParam Map<String, String> form, Authentication auth, RedirectAttributes ra) {
        UserRole role = UserRole.valueOf(form.getOrDefault("role", "SOLICITANTE"));

        userAccountService.create(
                form.get("username"),
                form.get("email"),
                form.get("senha"),
                role,
                nullableLong(form.get("unidade_id")),
                currentUserService.id(auth));

        ra.addFlashAttribute("ok", "Usuario criado.");
        return "redirect:/usuarios";
    }

    @PostMapping("/usuarios/{id}")
    public String editar(@PathVariable Long id,
            @RequestParam Map<String, String> form,
            Authentication auth,
            RedirectAttributes ra) {
        userAccountService.update(
                id,
                form.get("email"),
                form.get("senha"),
                UserRole.valueOf(form.get("role")),
                nullableLong(form.get("unidade_id")),
                "on".equals(form.get("ativo")),
                currentUserService.id(auth));
        ra.addFlashAttribute("ok", "Usuario atualizado.");
        return "redirect:/usuarios";
    }

    @PostMapping("/usuarios/{id}/delete")
    public String deletar(@PathVariable Long id, Authentication auth, RedirectAttributes ra) {
        if (currentUserService.id(auth).equals(id)) {
            ra.addFlashAttribute("erro", "Voce nao pode excluir o proprio usuario logado.");
            return "redirect:/usuarios";
        }
        userAccountService.delete(id, currentUserService.id(auth));
        ra.addFlashAttribute("ok", "Usuario excluido.");
        return "redirect:/usuarios";
    }

    private Long nullableLong(String value) {
        return value == null || value.isBlank() ? null : Long.valueOf(value);
    }
}
