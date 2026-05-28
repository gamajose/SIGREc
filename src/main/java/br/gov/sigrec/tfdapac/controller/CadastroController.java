package br.gov.sigrec.tfdapac.controller;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.sql.Date;
import java.time.LocalDate;
import java.util.Map;

@Controller
public class CadastroController {

    private final JdbcTemplate jdbcTemplate;

    public CadastroController(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @GetMapping("/pacientes")
    public String pacientes(@RequestParam(defaultValue = "") String q, Model model) {
        model.addAttribute("q", q);
        model.addAttribute("itens", jdbcTemplate.queryForList("""
                select * from regulacao_tfd.pacientes
                where ? = '' or nome ilike ? or cns ilike ? or cpf ilike ?
                order by nome limit 100
                """, q, like(q), like(q), like(q)));
        return "pacientes/list";
    }

    @PostMapping("/pacientes")
    public String salvarPaciente(@RequestParam Map<String, String> f, RedirectAttributes ra) {
        jdbcTemplate.update("""
                insert into regulacao_tfd.pacientes
                (nome, cns, cpf, rg, data_nascimento, sexo, raca_cor, nome_mae, endereco, bairro, municipio, uf, cep, telefone, email, responsavel)
                values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """, f.get("nome"), digits(f.get("cns")), digits(f.get("cpf")), f.get("rg"), date(f.get("data_nascimento")),
                f.get("sexo"), f.get("raca_cor"), f.get("nome_mae"), f.get("endereco"), f.get("bairro"),
                f.get("municipio"), f.get("uf"), digits(f.get("cep")), digits(f.get("telefone")), f.get("email"), f.get("responsavel"));
        ra.addFlashAttribute("ok", "Paciente cadastrado.");
        return "redirect:/pacientes";
    }

    @GetMapping("/unidades")
    public String unidades(Model model) {
        model.addAttribute("itens", jdbcTemplate.queryForList("select * from regulacao_tfd.unidades_saude order by nome"));
        return "unidades/list";
    }

    @PreAuthorize("hasAnyRole('ADMIN','REGULADOR')")
    @PostMapping("/unidades")
    public String salvarUnidade(@RequestParam Map<String, String> f, RedirectAttributes ra) {
        jdbcTemplate.update("""
                insert into regulacao_tfd.unidades_saude (nome, cnes, municipio, uf, telefone, responsavel, ativo)
                values (?, ?, ?, ?, ?, ?, true)
                """, f.get("nome"), digits(f.get("cnes")), f.get("municipio"), f.get("uf"), digits(f.get("telefone")), f.get("responsavel"));
        jdbcTemplate.update("""
                update regulacao_tfd.unidades_saude
                set tipo_unidade = ?
                where id = (select max(id) from regulacao_tfd.unidades_saude)
                """, f.getOrDefault("tipo_unidade", "SOLICITANTE"));
        ra.addFlashAttribute("ok", "Unidade cadastrada.");
        return "redirect:/unidades";
    }

    @GetMapping("/profissionais")
    public String profissionais(Model model) {
        model.addAttribute("itens", jdbcTemplate.queryForList("""
                select p.*, u.nome unidade_nome
                from regulacao_tfd.profissionais p
                left join regulacao_tfd.unidades_saude u on u.id = p.unidade_id
                order by p.nome
                """));
        model.addAttribute("unidades", jdbcTemplate.queryForList("select id, nome from regulacao_tfd.unidades_saude order by nome"));
        return "profissionais/list";
    }

    @PreAuthorize("hasAnyRole('ADMIN','REGULADOR','SOLICITANTE')")
    @PostMapping("/profissionais")
    public String salvarProfissional(@RequestParam Map<String, String> f, Authentication authentication, RedirectAttributes ra) {
        boolean podeDefinirAtuacao = hasRole(authentication, "ADMIN") || hasRole(authentication, "REGULADOR");
        boolean solicitante = podeDefinirAtuacao ? "on".equals(f.get("solicitante")) : true;
        boolean autorizador = podeDefinirAtuacao && "on".equals(f.get("autorizador"));

        jdbcTemplate.update("""
            insert into regulacao_tfd.profissionais
            (nome, cpf_cns, conselho, registro_conselho, especialidade, unidade_id, solicitante, autorizador, ativo)
            values (?, ?, ?, ?, ?, ?, ?, ?, true)
            """, f.get("nome"), digits(f.get("cpf_cns")), f.get("conselho"), f.get("registro_conselho"),
                f.get("especialidade"), nullableLong(f.get("unidade_id")), solicitante, autorizador);
        ra.addFlashAttribute("ok", "Profissional cadastrado.");
        return "redirect:/profissionais";
    }

    @GetMapping("/procedimentos")
    public String procedimentos(@RequestParam(defaultValue = "") String q,
            @RequestParam(defaultValue = "") String origem,
            Model model) {
        model.addAttribute("q", q);
        model.addAttribute("origem", origem);

        String busca = like(q);

        model.addAttribute("itens", jdbcTemplate.queryForList("""
            select codigo, descricao, tipo, valor, ativo, origem
            from regulacao_tfd.vw_procedimentos_unificados
            where (? = '' or codigo ilike ? or descricao ilike ?)
              and (? = '' or origem = ?)
            order by descricao
            limit 100
            """,
                q, busca, busca,
                origem, origem));

        return "procedimentos/list";
    }

    @PreAuthorize("hasAnyRole('ADMIN','REGULADOR')")
    @PostMapping("/procedimentos")
    public String salvarProcedimento(@RequestParam Map<String, String> f, RedirectAttributes ra) {
        jdbcTemplate.update("""
                insert into regulacao_tfd.procedimentos (codigo, descricao, tipo, valor, origem, ativo)
                values (?, ?, ?, ?, 'manual', true)
                on conflict (codigo) do update set descricao = excluded.descricao, tipo = excluded.tipo, valor = excluded.valor
                """, f.get("codigo"), f.get("descricao"), f.get("tipo"), decimal(f.get("valor")));
        ra.addFlashAttribute("ok", "Procedimento salvo.");
        return "redirect:/procedimentos";
    }

    @PreAuthorize("hasAnyRole('ADMIN','REGULADOR')")
    @PostMapping("/procedimentos/{id}/toggle")
    public String toggleProcedimento(@PathVariable Long id) {
        jdbcTemplate.update("update regulacao_tfd.procedimentos set ativo = not ativo where id = ?", id);
        return "redirect:/procedimentos";
    }

    private String like(String q) {
        return "%" + q + "%";
    }

    private Date date(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        String v = value.trim();

        try {
            // Formato padrão do input type="date": 1992-06-25
            if (v.matches("\\d{4}-\\d{2}-\\d{2}")) {
                return Date.valueOf(LocalDate.parse(v));
            }

            // Remove qualquer coisa que não seja número.
            // Aceita: 25061992, 25/06/1992, 25-06-1992, 250692, 25/06/92
            String numeros = v.replaceAll("\\D", "");

            if (numeros.length() == 8) {
                int dia = Integer.parseInt(numeros.substring(0, 2));
                int mes = Integer.parseInt(numeros.substring(2, 4));
                int ano = Integer.parseInt(numeros.substring(4, 8));

                return Date.valueOf(LocalDate.of(ano, mes, dia));
            }

            if (numeros.length() == 6) {
                int dia = Integer.parseInt(numeros.substring(0, 2));
                int mes = Integer.parseInt(numeros.substring(2, 4));
                int anoCurto = Integer.parseInt(numeros.substring(4, 6));

                // Regra:
                // 00 a 29 = 2000 a 2029
                // 30 a 99 = 1930 a 1999
                int ano = anoCurto <= 29 ? 2000 + anoCurto : 1900 + anoCurto;

                return Date.valueOf(LocalDate.of(ano, mes, dia));
            }

            return null;
        } catch (Exception e) {
            return null;
        }
    }

    private Long nullableLong(String value) {
        return value == null || value.isBlank() ? null : Long.valueOf(value);
    }

    private String digits(String value) {
        return value == null ? null : value.replaceAll("\\D", "");
    }

    private boolean hasRole(Authentication authentication, String role) {
        if (authentication == null) {
            return false;
        }
        String authority = "ROLE_" + role;
        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(authority::equals);
    }

    private java.math.BigDecimal decimal(String value) {
        return value == null || value.isBlank() ? null : new java.math.BigDecimal(value.replace(",", "."));
    }
}
