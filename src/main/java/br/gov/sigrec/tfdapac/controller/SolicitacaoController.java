package br.gov.sigrec.tfdapac.controller;

import org.springframework.web.bind.annotation.ResponseBody;
import br.gov.sigrec.tfdapac.service.CurrentUserService;
import br.gov.sigrec.tfdapac.service.LookupService;
import br.gov.sigrec.tfdapac.service.SolicitacaoService;
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
public class SolicitacaoController {

    private final SolicitacaoService solicitacaoService;
    private final LookupService lookupService;
    private final CurrentUserService currentUserService;

    public SolicitacaoController(SolicitacaoService solicitacaoService, LookupService lookupService, CurrentUserService currentUserService) {
        this.solicitacaoService = solicitacaoService;
        this.lookupService = lookupService;
        this.currentUserService = currentUserService;
    }

    @GetMapping("/solicitacoes")
    public String minhas(@RequestParam(defaultValue = "") String status,
            @RequestParam(defaultValue = "") String tipo,
            @RequestParam(defaultValue = "") String q,
            Authentication auth,
            Model model) {
        model.addAttribute("itens", solicitacaoService.filaDoUsuario(
                status,
                tipo,
                q,
                currentUserService.id(auth),
                currentUserService.unidadeId(auth),
                currentUserService.isAdmin(auth)));
        model.addAttribute("status", status);
        model.addAttribute("tipo", tipo);
        model.addAttribute("q", q);
        model.addAttribute("titulo", "Solicitacoes");
        return "solicitacoes/list";
    }

    @GetMapping("/solicitacoes/nova/{tipo}")
    public String nova(@PathVariable String tipo, @RequestParam(defaultValue = "") String q, Model model) {
        model.addAttribute("tipo", tipo.toUpperCase());
        model.addAttribute("pacientes", lookupService.pacientes(q));
        model.addAttribute("unidades", lookupService.unidades());
        model.addAttribute("unidadesAutorizadoras", lookupService.unidadesAutorizadoras());
        model.addAttribute("profissionais", lookupService.profissionais());
        model.addAttribute("procedimentos", lookupService.procedimentos(""));
        return "solicitacoes/form";
    }

    @GetMapping("/api/pacientes")
    @ResponseBody
    public java.util.List<java.util.Map<String, Object>> buscarPacientes(@RequestParam(defaultValue = "") String q) {
        return lookupService.pacientes(q);
    }

    @GetMapping("/api/procedimentos")
    @ResponseBody
    public java.util.List<java.util.Map<String, Object>> buscarProcedimentos(@RequestParam(defaultValue = "") String q) {
        return lookupService.procedimentos(q);
    }

    @GetMapping("/api/profissionais")
    @ResponseBody
    public java.util.List<java.util.Map<String, Object>> buscarProfissionais(@RequestParam(defaultValue = "") String q) {
        return lookupService.profissionais(q);
    }

    @PostMapping("/solicitacoes")
    public String criar(@RequestParam Map<String, String> form, Authentication auth, RedirectAttributes ra) {
        Long id = solicitacaoService.criar(form, currentUserService.id(auth));
        ra.addFlashAttribute("ok", "Solicitacao enviada para regulacao.");
        return "redirect:/solicitacoes/" + id;
    }

    @GetMapping("/solicitacoes/{id}")
    public String detalhe(@PathVariable Long id, Model model, Authentication auth) {
        if (!solicitacaoService.podeAcessar(id, currentUserService.id(auth), currentUserService.unidadeId(auth), currentUserService.isAdmin(auth))) {
            return "redirect:/solicitacoes";
        }
        var solicitacao = solicitacaoService.detalhe(id);
        model.addAttribute("solicitacao", solicitacao);
        if ("TFD".equals(solicitacao.get("tipo_solicitacao"))) {
            model.addAttribute("complemento", solicitacaoService.tfd(id));
        } else {
            model.addAttribute("complemento", solicitacaoService.apac(id));
        }
        model.addAttribute("historico", solicitacaoService.historico(id));
        model.addAttribute("anexos", solicitacaoService.anexos(id));
        return "solicitacoes/detail";
    }
}
