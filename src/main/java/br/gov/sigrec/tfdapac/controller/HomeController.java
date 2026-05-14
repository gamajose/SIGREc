package br.gov.sigrec.tfdapac.controller;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {
    private final JdbcTemplate jdbcTemplate;

    public HomeController(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @GetMapping("/")
    public String dashboard(Model model) {
        model.addAttribute("contadores", jdbcTemplate.queryForList("""
                select status, count(*) total
                from regulacao_tfd.solicitacoes
                group by status
                order by status
                """));
        model.addAttribute("recentes", jdbcTemplate.queryForList("""
                select s.id, s.numero_protocolo, s.tipo_solicitacao, s.prioridade, s.status, s.data_entrada, p.nome paciente_nome
                from regulacao_tfd.solicitacoes s
                join regulacao_tfd.pacientes p on p.id = s.paciente_id
                order by s.data_entrada desc
                limit 10
                """));
        return "dashboard/index";
    }
}
