package br.gov.sigrec.tfdapac.controller;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import br.gov.sigrec.tfdapac.service.ProcedimentoTxtImportService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.sql.Date;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.Map;

@Controller
public class CadastroController {
    private final JdbcTemplate jdbcTemplate;
    private final ProcedimentoTxtImportService procedimentoTxtImportService;

    public CadastroController(JdbcTemplate jdbcTemplate, ProcedimentoTxtImportService procedimentoTxtImportService) {
        this.jdbcTemplate = jdbcTemplate;
        this.procedimentoTxtImportService = procedimentoTxtImportService;
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
                (nome, cns, cpf, rg, data_nascimento, sexo, raca_cor, nome_mae, endereco, municipio, uf, cep, telefone, email, responsavel)
                values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """, f.get("nome"), f.get("cns"), f.get("cpf"), f.get("rg"), date(f.get("data_nascimento")),
                f.get("sexo"), f.get("raca_cor"), f.get("nome_mae"), f.get("endereco"), f.get("municipio"),
                f.get("uf"), f.get("cep"), f.get("telefone"), f.get("email"), f.get("responsavel"));
        ra.addFlashAttribute("ok", "Paciente cadastrado.");
        return "redirect:/pacientes";
    }

    @GetMapping("/unidades")
    public String unidades(Model model) {
        model.addAttribute("itens", jdbcTemplate.queryForList("select * from regulacao_tfd.unidades_saude order by nome"));
        return "unidades/list";
    }

    @PreAuthorize("hasAnyRole('ADMIN','REGULACAO')")
    @PostMapping("/unidades")
    public String salvarUnidade(@RequestParam Map<String, String> f, RedirectAttributes ra) {
        jdbcTemplate.update("""
                insert into regulacao_tfd.unidades_saude (nome, cnes, municipio, uf, telefone, responsavel, ativo)
                values (?, ?, ?, ?, ?, ?, true)
                """, f.get("nome"), f.get("cnes"), f.get("municipio"), f.get("uf"), f.get("telefone"), f.get("responsavel"));
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

    @PreAuthorize("hasAnyRole('ADMIN','REGULACAO')")
    @PostMapping("/profissionais")
    public String salvarProfissional(@RequestParam Map<String, String> f, RedirectAttributes ra) {
        jdbcTemplate.update("""
                insert into regulacao_tfd.profissionais
                (nome, cpf_cns, conselho, registro_conselho, especialidade, unidade_id, solicitante, autorizador, ativo)
                values (?, ?, ?, ?, ?, ?, ?, ?, true)
                """, f.get("nome"), f.get("cpf_cns"), f.get("conselho"), f.get("registro_conselho"),
                f.get("especialidade"), nullableLong(f.get("unidade_id")), "on".equals(f.get("solicitante")),
                "on".equals(f.get("autorizador")));
        ra.addFlashAttribute("ok", "Profissional cadastrado.");
        return "redirect:/profissionais";
    }

    @GetMapping("/procedimentos")
    public String procedimentos(@RequestParam(defaultValue = "") String q, Model model) {
        model.addAttribute("q", q);
        model.addAttribute("itens", jdbcTemplate.queryForList("""
                select * from regulacao_tfd.procedimentos
                where ? = '' or codigo ilike ? or descricao ilike ?
                order by descricao limit 100
                """, q, like(q), like(q)));
        return "procedimentos/list";
    }

    @PreAuthorize("hasAnyRole('ADMIN','REGULACAO')")
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

    @PreAuthorize("hasAnyRole('ADMIN','REGULACAO')")
    @PostMapping("/procedimentos/{id}/toggle")
    public String toggleProcedimento(@PathVariable Long id) {
        jdbcTemplate.update("update regulacao_tfd.procedimentos set ativo = not ativo where id = ?", id);
        return "redirect:/procedimentos";
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/procedimentos/importar-txt")
    public String importarProcedimentosTxt(RedirectAttributes ra) {
        int total = procedimentoTxtImportService.importar0202(Path.of("tb_procedimento.txt"));
        ra.addFlashAttribute("ok", total + " procedimentos 0202 importados do tb_procedimento.txt.");
        return "redirect:/procedimentos?q=0202";
    }

    private String like(String q) {
        return "%" + q + "%";
    }

    private Date date(String value) {
        return value == null || value.isBlank() ? null : Date.valueOf(LocalDate.parse(value));
    }

    private Long nullableLong(String value) {
        return value == null || value.isBlank() ? null : Long.valueOf(value);
    }

    private java.math.BigDecimal decimal(String value) {
        return value == null || value.isBlank() ? null : new java.math.BigDecimal(value.replace(",", "."));
    }
}
