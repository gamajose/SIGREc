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
import org.springframework.web.bind.annotation.ResponseBody;
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

    @GetMapping("/api/unidades/cnes/{cnes}")
    @ResponseBody
    public Map<String, Object> unidadePorCnes(@PathVariable String cnes) {
        var lista = jdbcTemplate.queryForList("""
                select id, nome, cnes, municipio, uf, telefone, responsavel, tipo_unidade
                from regulacao_tfd.unidades_saude
                where regexp_replace(coalesce(cnes, ''), '\\D', '', 'g') = ?
                order by id desc
                limit 1
                """, digits(cnes));
        return lista.isEmpty() ? Map.of() : lista.get(0);
    }

    @GetMapping("/api/profissionais/conselho")
    @ResponseBody
    public Map<String, Object> profissionalPorConselho(@RequestParam String conselho,
                                                       @RequestParam String registro,
                                                       @RequestParam(defaultValue = "") String uf) {
        var lista = jdbcTemplate.queryForList("""
                select id, nome, cpf_cns, conselho, registro_conselho, uf_conselho, especialidade, unidade_id
                from regulacao_tfd.profissionais
                where upper(coalesce(conselho, '')) = upper(?)
                  and regexp_replace(coalesce(registro_conselho, ''), '\\D', '', 'g') = ?
                  and (? = '' or upper(coalesce(uf_conselho, '')) = upper(?))
                order by id desc
                limit 1
                """, conselho, digits(registro), uf, uf);
        return lista.isEmpty() ? Map.of() : lista.get(0);
    }

    @GetMapping("/pacientes")
    public String pacientes(@RequestParam(defaultValue = "") String q,
                            @RequestParam(defaultValue = "") String municipio,
                            @RequestParam(defaultValue = "") String uf,
                            @RequestParam(defaultValue = "") String dataInicio,
                            @RequestParam(defaultValue = "") String dataFim,
                            @RequestParam(defaultValue = "1") Integer page,
                            Model model) {
        int pageSize = 20;
        int paginaAtual = Math.max(page == null ? 1 : page, 1);
        int offset = (paginaAtual - 1) * pageSize;

        Integer total = jdbcTemplate.queryForObject("""
                select count(*)
                from regulacao_tfd.pacientes
                where (? = '' or nome ilike ? or cns ilike ? or cpf ilike ? or municipio ilike ?)
                  and (? = '' or municipio = ?)
                  and (? = '' or uf = ?)
                  and (? = '' or data_nascimento >= ?::date)
                  and (? = '' or data_nascimento <= ?::date)
                """, Integer.class, q, like(q), like(q), like(q), like(q), municipio, municipio, uf, uf, dataInicio, dataInicio, dataFim, dataFim);
        int totalRegistros = total == null ? 0 : total;
        int totalPaginas = Math.max((int) Math.ceil(totalRegistros / (double) pageSize), 1);
        if (paginaAtual > totalPaginas) paginaAtual = totalPaginas;
        offset = (paginaAtual - 1) * pageSize;

        model.addAttribute("q", q);
        model.addAttribute("municipio", municipio);
        model.addAttribute("uf", uf);
        model.addAttribute("dataInicio", dataInicio);
        model.addAttribute("dataFim", dataFim);
        model.addAttribute("page", paginaAtual);
        model.addAttribute("totalPaginas", totalPaginas);
        model.addAttribute("totalRegistros", totalRegistros);
        model.addAttribute("temAnterior", paginaAtual > 1);
        model.addAttribute("temProxima", paginaAtual < totalPaginas);
        model.addAttribute("municipios", jdbcTemplate.queryForList("select distinct municipio from regulacao_tfd.pacientes where municipio is not null and municipio <> '' order by municipio"));
        model.addAttribute("ufs", jdbcTemplate.queryForList("select distinct uf from regulacao_tfd.pacientes where uf is not null and uf <> '' order by uf"));
        model.addAttribute("itens", jdbcTemplate.queryForList("""
                select (? + row_number() over (order by nome)) numero, *
                from regulacao_tfd.pacientes
                where (? = '' or nome ilike ? or cns ilike ? or cpf ilike ? or municipio ilike ?)
                  and (? = '' or municipio = ?)
                  and (? = '' or uf = ?)
                  and (? = '' or data_nascimento >= ?::date)
                  and (? = '' or data_nascimento <= ?::date)
                order by nome
                limit ? offset ?
                """, offset, q, like(q), like(q), like(q), like(q), municipio, municipio, uf, uf, dataInicio, dataInicio, dataFim, dataFim, pageSize, offset));
        return "pacientes/list";
    }

    @GetMapping("/pacientes/{id}")
    public String detalhePaciente(@PathVariable Long id, Model model) {
        model.addAttribute("paciente", jdbcTemplate.queryForMap("select * from regulacao_tfd.pacientes where id = ?", id));
        model.addAttribute("resumo", jdbcTemplate.queryForMap("""
                select count(*) total,
                       count(*) filter (where status = 'AUTORIZADA') autorizadas,
                       count(*) filter (where status = 'INDEFERIDA') indeferidas,
                       count(*) filter (where status = 'AGUARDANDO_DOCUMENTOS') aguardando_documentos,
                       count(*) filter (where status in ('ENVIADA','EM_ANALISE')) em_andamento,
                       coalesce(to_char(max(data_entrada), 'DD/MM/YYYY HH24:MI:SS'), '-') ultima_movimentacao
                from regulacao_tfd.solicitacoes
                where paciente_id = ?
                """, id));
        model.addAttribute("solicitacoes", jdbcTemplate.queryForList("""
                select s.id, s.numero_protocolo, s.numero_solicitacao, s.tipo_solicitacao, s.status, s.prioridade,
                       coalesce(to_char(s.data_entrada, 'DD/MM/YYYY HH24:MI:SS'), '-') entrada_formatada,
                       u.nome unidade_nome
                from regulacao_tfd.solicitacoes s
                left join regulacao_tfd.unidades_saude u on u.id = s.unidade_solicitante_id
                where s.paciente_id = ?
                order by s.data_entrada desc
                """, id));
        return "pacientes/detalhe";
    }

    @PostMapping("/pacientes")
    public String salvarPaciente(@RequestParam Map<String, String> f, RedirectAttributes ra) {
        String duplicidade = verificarDuplicidadePaciente(f);
        if (duplicidade != null) {
            ra.addFlashAttribute("erro", duplicidade);
            return "redirect:/pacientes";
        }
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

    private String verificarDuplicidadePaciente(Map<String, String> f) {
        String cpf = digits(f.get("cpf"));
        if (cpf != null && !cpf.isBlank()) {
            var encontrado = jdbcTemplate.queryForList("select id, nome from regulacao_tfd.pacientes where cpf = ? limit 1", cpf);
            if (!encontrado.isEmpty()) return "Paciente possivelmente duplicado: CPF ja cadastrado para " + encontrado.get(0).get("nome") + ".";
        }
        String cns = digits(f.get("cns"));
        if (cns != null && !cns.isBlank()) {
            var encontrado = jdbcTemplate.queryForList("select id, nome from regulacao_tfd.pacientes where cns = ? limit 1", cns);
            if (!encontrado.isEmpty()) return "Paciente possivelmente duplicado: CNS ja cadastrado para " + encontrado.get(0).get("nome") + ".";
        }
        Date nascimento = date(f.get("data_nascimento"));
        String nome = normalizar(f.get("nome"));
        String mae = normalizar(f.get("nome_mae"));
        if (!nome.isBlank() && !mae.isBlank() && nascimento != null) {
            var encontrado = jdbcTemplate.queryForList("""
                    select id, nome
                    from regulacao_tfd.pacientes
                    where upper(trim(nome)) = upper(trim(?))
                      and data_nascimento = ?
                      and upper(trim(coalesce(nome_mae, ''))) = upper(trim(?))
                    limit 1
                    """, nome, nascimento, mae);
            if (!encontrado.isEmpty()) return "Paciente possivelmente duplicado: nome, nascimento e nome da mae ja constam para " + encontrado.get(0).get("nome") + ".";
        }
        return null;
    }

    @GetMapping("/unidades")
    public String unidades(@RequestParam(defaultValue = "") String q,
                           @RequestParam(defaultValue = "") String tipo,
                           @RequestParam(defaultValue = "") String uf,
                           @RequestParam(defaultValue = "1") Integer page,
                           Model model) {
        int pageSize = 20;
        int paginaAtual = Math.max(page == null ? 1 : page, 1);
        int offset = (paginaAtual - 1) * pageSize;
        Integer total = jdbcTemplate.queryForObject("""
                select count(*)
                from regulacao_tfd.unidades_saude
                where (? = '' or nome ilike ? or cnes ilike ? or municipio ilike ?)
                  and (? = '' or tipo_unidade = ?)
                  and (? = '' or uf = ?)
                """, Integer.class, q, like(q), like(q), like(q), tipo, tipo, uf, uf);
        int totalRegistros = total == null ? 0 : total;
        int totalPaginas = Math.max((int) Math.ceil(totalRegistros / (double) pageSize), 1);
        if (paginaAtual > totalPaginas) paginaAtual = totalPaginas;
        offset = (paginaAtual - 1) * pageSize;

        model.addAttribute("q", q);
        model.addAttribute("tipo", tipo);
        model.addAttribute("uf", uf);
        model.addAttribute("page", paginaAtual);
        model.addAttribute("totalPaginas", totalPaginas);
        model.addAttribute("totalRegistros", totalRegistros);
        model.addAttribute("temAnterior", paginaAtual > 1);
        model.addAttribute("temProxima", paginaAtual < totalPaginas);
        model.addAttribute("tiposUnidade", jdbcTemplate.queryForList("select distinct tipo_unidade from regulacao_tfd.unidades_saude where tipo_unidade is not null order by tipo_unidade"));
        model.addAttribute("ufsUnidade", jdbcTemplate.queryForList("select distinct uf from regulacao_tfd.unidades_saude where uf is not null and uf <> '' order by uf"));
        model.addAttribute("itens", jdbcTemplate.queryForList("""
                select (? + row_number() over (order by nome)) numero, *
                from regulacao_tfd.unidades_saude
                where (? = '' or nome ilike ? or cnes ilike ? or municipio ilike ?)
                  and (? = '' or tipo_unidade = ?)
                  and (? = '' or uf = ?)
                order by nome
                limit ? offset ?
                """, offset, q, like(q), like(q), like(q), tipo, tipo, uf, uf, pageSize, offset));
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
    public String profissionais(@RequestParam(defaultValue = "") String q,
                                @RequestParam(defaultValue = "") String conselho,
                                @RequestParam(defaultValue = "") String uf,
                                @RequestParam(defaultValue = "1") Integer page,
                                Model model) {
        int pageSize = 20;
        int paginaAtual = Math.max(page == null ? 1 : page, 1);
        int offset = (paginaAtual - 1) * pageSize;
        Integer total = jdbcTemplate.queryForObject("""
                select count(*)
                from regulacao_tfd.profissionais p
                where (? = '' or p.nome ilike ? or p.cpf_cns ilike ? or p.registro_conselho ilike ?)
                  and (? = '' or p.conselho = ?)
                  and (? = '' or p.uf_conselho = ?)
                """, Integer.class, q, like(q), like(q), like(q), conselho, conselho, uf, uf);
        int totalRegistros = total == null ? 0 : total;
        int totalPaginas = Math.max((int) Math.ceil(totalRegistros / (double) pageSize), 1);
        if (paginaAtual > totalPaginas) paginaAtual = totalPaginas;
        offset = (paginaAtual - 1) * pageSize;

        model.addAttribute("q", q);
        model.addAttribute("conselho", conselho);
        model.addAttribute("uf", uf);
        model.addAttribute("page", paginaAtual);
        model.addAttribute("totalPaginas", totalPaginas);
        model.addAttribute("totalRegistros", totalRegistros);
        model.addAttribute("temAnterior", paginaAtual > 1);
        model.addAttribute("temProxima", paginaAtual < totalPaginas);
        model.addAttribute("conselhos", jdbcTemplate.queryForList("select distinct conselho from regulacao_tfd.profissionais where conselho is not null and conselho <> '' order by conselho"));
        model.addAttribute("ufs", jdbcTemplate.queryForList("select distinct uf_conselho from regulacao_tfd.profissionais where uf_conselho is not null and uf_conselho <> '' order by uf_conselho"));
        model.addAttribute("itens", jdbcTemplate.queryForList("""
                select (? + row_number() over (order by p.nome)) numero,
                       p.*, u.nome unidade_nome
                from regulacao_tfd.profissionais p
                left join regulacao_tfd.unidades_saude u on u.id = p.unidade_id
                where (? = '' or p.nome ilike ? or p.cpf_cns ilike ? or p.registro_conselho ilike ?)
                  and (? = '' or p.conselho = ?)
                  and (? = '' or p.uf_conselho = ?)
                order by p.nome
                limit ? offset ?
                """, offset, q, like(q), like(q), like(q), conselho, conselho, uf, uf, pageSize, offset));
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
            """, q, busca, busca, origem, origem));
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
        return "%" + (q == null ? "" : q.trim()) + "%";
    }

    private Date date(String value) {
        if (value == null || value.isBlank()) return null;
        String v = value.trim();
        try {
            if (v.matches("\\d{4}-\\d{2}-\\d{2}")) return Date.valueOf(LocalDate.parse(v));
            String numeros = v.replaceAll("\\D", "");
            if (numeros.length() == 8) return Date.valueOf(LocalDate.of(Integer.parseInt(numeros.substring(4, 8)), Integer.parseInt(numeros.substring(2, 4)), Integer.parseInt(numeros.substring(0, 2))));
            if (numeros.length() == 6) {
                int anoCurto = Integer.parseInt(numeros.substring(4, 6));
                int ano = anoCurto <= 29 ? 2000 + anoCurto : 1900 + anoCurto;
                return Date.valueOf(LocalDate.of(ano, Integer.parseInt(numeros.substring(2, 4)), Integer.parseInt(numeros.substring(0, 2))));
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

    private String normalizar(String value) {
        return value == null ? "" : value.trim();
    }

    private boolean hasRole(Authentication authentication, String role) {
        if (authentication == null) return false;
        String authority = "ROLE_" + role;
        return authentication.getAuthorities().stream().map(GrantedAuthority::getAuthority).anyMatch(authority::equals);
    }

    private java.math.BigDecimal decimal(String value) {
        return value == null || value.isBlank() ? null : new java.math.BigDecimal(value.replace(",", "."));
    }
}
