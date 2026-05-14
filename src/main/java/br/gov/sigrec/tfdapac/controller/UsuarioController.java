package br.gov.sigrec.tfdapac.controller;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.Map;

@Controller
@PreAuthorize("hasRole('ADMIN')")
public class UsuarioController {
    private final JdbcTemplate jdbcTemplate;
    private final PasswordEncoder passwordEncoder;

    public UsuarioController(JdbcTemplate jdbcTemplate, PasswordEncoder passwordEncoder) {
        this.jdbcTemplate = jdbcTemplate;
        this.passwordEncoder = passwordEncoder;
    }

    @GetMapping("/usuarios")
    public String list(Model model) {
        model.addAttribute("usuarios", jdbcTemplate.queryForList("""
                select u.*, us.nome unidade_nome,
                       string_agg(r.nome, ', ' order by r.nome) roles
                from regulacao_tfd.usuarios u
                left join regulacao_tfd.unidades_saude us on us.id = u.unidade_id
                left join regulacao_tfd.user_roles ur on ur.usuario_id = u.id
                left join regulacao_tfd.roles r on r.id = ur.role_id
                group by u.id, us.nome
                order by u.nome
                """));
        model.addAttribute("roles", jdbcTemplate.queryForList("select * from regulacao_tfd.roles order by nome"));
        model.addAttribute("unidades", jdbcTemplate.queryForList("select id, nome from regulacao_tfd.unidades_saude order by nome"));
        return "usuarios/list";
    }

    @PostMapping("/usuarios")
    public String criar(@RequestParam Map<String, String> form,
                        @RequestParam(name = "roles", required = false) List<Long> roles,
                        RedirectAttributes ra) {
        Long id = jdbcTemplate.queryForObject("""
                insert into regulacao_tfd.usuarios (nome, username, senha_hash, unidade_id, ativo)
                values (?, ?, ?, ?, true)
                returning id
                """, Long.class, form.get("nome"), form.get("username"), passwordEncoder.encode(form.get("senha")),
                form.get("unidade_id") == null || form.get("unidade_id").isBlank() ? null : Long.valueOf(form.get("unidade_id")));
        if (roles != null) {
            for (Long roleId : roles) {
                jdbcTemplate.update("insert into regulacao_tfd.user_roles (usuario_id, role_id) values (?, ?) on conflict do nothing", id, roleId);
            }
        }
        ra.addFlashAttribute("ok", "Usuario criado.");
        return "redirect:/usuarios";
    }

    @PostMapping("/usuarios/{id}/toggle")
    public String toggle(@PathVariable Long id) {
        jdbcTemplate.update("update regulacao_tfd.usuarios set ativo = not ativo where id = ?", id);
        return "redirect:/usuarios";
    }
}
