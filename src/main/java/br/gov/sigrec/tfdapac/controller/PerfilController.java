package br.gov.sigrec.tfdapac.controller;

import br.gov.sigrec.tfdapac.service.CurrentUserService;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
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

    @GetMapping("/perfil/completo")
    public String completo(Authentication auth, Model model) {
        Long id = currentUserService.id(auth);
        model.addAttribute("perfil", jdbcTemplate.queryForMap("""
                select u.*,
                       us.nome unidade_nome
                from regulacao_tfd.usuarios u
                left join regulacao_tfd.unidades_saude us on us.id = u.unidade_id
                where u.id = ?
                """, id));
        return "perfil/completo";
    }

    @PostMapping("/perfil/completo")
    public String salvarCompleto(@RequestParam Map<String, String> form, Authentication auth, RedirectAttributes ra) {
        Long id = currentUserService.id(auth);
        String senha = form.get("senha");

        if (StringUtils.hasText(senha)) {
            String hash = passwordEncoder.encode(senha);
            jdbcTemplate.update("""
                    update regulacao_tfd.usuarios
                    set nome = ?,
                        email = ?,
                        telefone = ?,
                        codigo_usuario = ?,
                        unidade_telefone = ?,
                        unidade_endereco = ?,
                        unidade_cep = ?,
                        unidade_numero = ?,
                        unidade_bairro = ?,
                        unidade_complemento = ?,
                        password_hash = ?,
                        senha_hash = ?,
                        perfil_completo = true,
                        primeiro_login = false,
                        data_perfil_atualizado = current_timestamp,
                        updated_at = current_timestamp
                    where id = ?
                    """,
                    form.get("nome"),
                    form.get("email"),
                    digits(form.get("telefone")),
                    blankToNull(form.get("codigo_usuario")),
                    digits(form.get("unidade_telefone")),
                    form.get("unidade_endereco"),
                    digits(form.get("unidade_cep")),
                    form.get("unidade_numero"),
                    form.get("unidade_bairro"),
                    form.get("unidade_complemento"),
                    hash,
                    hash,
                    id);
        } else {
            jdbcTemplate.update("""
                    update regulacao_tfd.usuarios
                    set nome = ?,
                        email = ?,
                        telefone = ?,
                        codigo_usuario = ?,
                        unidade_telefone = ?,
                        unidade_endereco = ?,
                        unidade_cep = ?,
                        unidade_numero = ?,
                        unidade_bairro = ?,
                        unidade_complemento = ?,
                        perfil_completo = true,
                        primeiro_login = false,
                        data_perfil_atualizado = current_timestamp,
                        updated_at = current_timestamp
                    where id = ?
                    """,
                    form.get("nome"),
                    form.get("email"),
                    digits(form.get("telefone")),
                    blankToNull(form.get("codigo_usuario")),
                    digits(form.get("unidade_telefone")),
                    form.get("unidade_endereco"),
                    digits(form.get("unidade_cep")),
                    form.get("unidade_numero"),
                    form.get("unidade_bairro"),
                    form.get("unidade_complemento"),
                    id);
        }

        ra.addFlashAttribute("ok", "Perfil completo atualizado.");
        return "redirect:/";
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

    private String digits(String value) {
        return value == null ? null : value.replaceAll("\\D", "");
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
