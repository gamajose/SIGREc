package br.gov.sigrec.tfdapac.controller;

import br.gov.sigrec.tfdapac.service.CurrentUserService;
import br.gov.sigrec.tfdapac.service.LookupService;
import br.gov.sigrec.tfdapac.service.SolicitacaoService;
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
public class RegulacaoController {
    private final SolicitacaoService solicitacaoService;
    private final LookupService lookupService;
    private final CurrentUserService currentUserService;

    public RegulacaoController(SolicitacaoService solicitacaoService, LookupService lookupService, CurrentUserService currentUserService) {
        this.solicitacaoService = solicitacaoService;
        this.lookupService = lookupService;
        this.currentUserService = currentUserService;
    }

    @GetMapping("/regulacao/fila")
    public String fila(@RequestParam(defaultValue = "") String status,
                       @RequestParam(defaultValue = "") String tipo,
                       @RequestParam(defaultValue = "") String q,
                       Model model) {
        model.addAttribute("itens", solicitacaoService.fila(status, tipo, q));
        model.addAttribute("status", status);
        model.addAttribute("tipo", tipo);
        model.addAttribute("q", q);
        model.addAttribute("titulo", "Fila de regulacao");
        return "regulacao/fila";
    }

    @GetMapping("/regulacao/{id}")
    public String analisar(@PathVariable Long id, Model model) {
        var solicitacao = solicitacaoService.detalhe(id);
        model.addAttribute("solicitacao", solicitacao);
        model.addAttribute("autorizadores", lookupService.autorizadores());
        model.addAttribute("historico", solicitacaoService.historico(id));
        return "regulacao/analise";
    }

    @PreAuthorize("hasAnyRole('ADMIN','REGULACAO','AUTORIZADOR')")
    @PostMapping("/regulacao/{id}/status")
    public String status(@PathVariable Long id,
                         @RequestParam String novo_status,
                         @RequestParam(required = false) String observacao,
                         @RequestParam Map<String, String> form,
                         Authentication auth,
                         RedirectAttributes ra) {
        solicitacaoService.mudarStatus(id, novo_status, observacao, currentUserService.id(auth), form);
        ra.addFlashAttribute("ok", "Status atualizado.");
        return "redirect:/regulacao/" + id;
    }
}
