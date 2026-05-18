package br.gov.sigrec.tfdapac.controller;

import br.gov.sigrec.tfdapac.service.EmailConfigService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Map;

@Controller
@PreAuthorize("hasRole('ADMIN')")
public class ParametroController {

    private final EmailConfigService emailConfigService;

    public ParametroController(EmailConfigService emailConfigService) {
        this.emailConfigService = emailConfigService;
    }

    @GetMapping("/parametros/email")
    public String email(Model model) {
        model.addAttribute("email", emailConfigService.buscar());
        return "parametros/email";
    }

    @PostMapping("/parametros/email")
    public String salvarEmail(@RequestParam Map<String, String> form, RedirectAttributes ra) {
        emailConfigService.salvar(form);
        ra.addFlashAttribute("ok", "Configuração de e-mail salva.");
        return "redirect:/parametros/email";
    }
}