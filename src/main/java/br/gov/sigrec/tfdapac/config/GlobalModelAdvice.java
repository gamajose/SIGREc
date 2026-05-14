package br.gov.sigrec.tfdapac.config;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import java.util.List;

@ControllerAdvice
public class GlobalModelAdvice {
    @ModelAttribute("usuarioLogado")
    public String usuarioLogado(Authentication authentication) {
        return authentication == null ? "" : authentication.getName();
    }

    @ModelAttribute("statusSolicitacao")
    public List<String> statusSolicitacao() {
        return List.of(
                "ENVIADA",
                "EM_ANALISE",
                "DEVOLVIDA_CORRECAO",
                "AGUARDANDO_DOCUMENTOS",
                "AUTORIZADA",
                "INDEFERIDA",
                "IMPRESSA",
                "CANCELADA",
                "FINALIZADA"
        );
    }
}
