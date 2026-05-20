package br.gov.sigrec.tfdapac.controller;

import br.gov.sigrec.tfdapac.service.CurrentUserService;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {
    private final JdbcTemplate jdbcTemplate;
    private final CurrentUserService currentUserService;

    public HomeController(JdbcTemplate jdbcTemplate, CurrentUserService currentUserService) {
        this.jdbcTemplate = jdbcTemplate;
        this.currentUserService = currentUserService;
    }

    @GetMapping("/")
    public String dashboard(Model model, Authentication auth) {
        boolean admin = currentUserService.isAdmin(auth);
        Long userId = currentUserService.id(auth);
        Long unidadeId = currentUserService.unidadeId(auth);

        model.addAttribute("contadores", admin ? jdbcTemplate.queryForList("""
                select status, count(*) total
                from regulacao_tfd.solicitacoes
                where numero_protocolo is not null
                group by status
                order by status
                """) : jdbcTemplate.queryForList("""
                select status, count(*) total
                from regulacao_tfd.solicitacoes
                where (usuario_criacao = ? or (? is not null and unidade_solicitante_id = ?))
                  and numero_protocolo is not null
                group by status
                order by status
                """, userId, unidadeId, unidadeId));

        List<Map<String, Object>> recentes = admin ? jdbcTemplate.queryForList("""
                select s.id, s.numero_protocolo, s.numero_solicitacao, s.tipo_solicitacao, s.prioridade, s.status, s.data_entrada, p.nome paciente_nome
                from regulacao_tfd.solicitacoes s
                join regulacao_tfd.pacientes p on p.id = s.paciente_id
                where s.numero_protocolo is not null
                order by s.data_entrada desc
                limit 25
                """) : jdbcTemplate.queryForList("""
                select s.id, s.numero_protocolo, s.numero_solicitacao, s.tipo_solicitacao, s.prioridade, s.status, s.data_entrada, p.nome paciente_nome
                from regulacao_tfd.solicitacoes s
                join regulacao_tfd.pacientes p on p.id = s.paciente_id
                where (s.usuario_criacao = ? or (? is not null and s.unidade_solicitante_id = ?))
                  and s.numero_protocolo is not null
                order by s.data_entrada desc
                limit 25
                """, userId, unidadeId, unidadeId);

        model.addAttribute("recentes", recentes.stream()
                .filter(this::registroValido)
                .limit(10)
                .toList());

        return "dashboard/index";
    }

    private boolean registroValido(Map<String, Object> item) {
        return textoValido(item.get("numero_protocolo"))
                && textoValido(item.get("paciente_nome"))
                && textoValido(item.get("tipo_solicitacao"))
                && textoValido(item.get("status"));
    }

    private boolean textoValido(Object valor) {
        if (valor == null) {
            return false;
        }
        String texto = valor.toString().toLowerCase();
        return !texto.contains("<!doctype")
                && !texto.contains("<html")
                && !texto.contains("<nav")
                && !texto.contains("ops, ocorreu um erro")
                && !texto.contains("internal server error");
    }
}
