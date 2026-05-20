package br.gov.sigrec.tfdapac.controller;

import br.gov.sigrec.tfdapac.service.CurrentUserService;
import br.gov.sigrec.tfdapac.service.PdfService;
import br.gov.sigrec.tfdapac.service.SolicitacaoService;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@Controller
@PreAuthorize("hasAnyRole('ADMIN', 'REGULADOR', 'SOLICITANTE')")
public class ImpressaoController {
    private final SolicitacaoService solicitacaoService;
    private final PdfService pdfService;
    private final CurrentUserService currentUserService;

    public ImpressaoController(SolicitacaoService solicitacaoService, PdfService pdfService, CurrentUserService currentUserService) {
        this.solicitacaoService = solicitacaoService;
        this.pdfService = pdfService;
        this.currentUserService = currentUserService;
    }

    @GetMapping("/impressao/{id}/visualizar")
    public String visualizar(@PathVariable Long id, Model model) {
        var solicitacao = solicitacaoService.detalhe(id);
        var complemento = "TFD".equals(solicitacao.get("tipo_solicitacao")) ? solicitacaoService.tfd(id) : solicitacaoService.apac(id);

        model.addAttribute("solicitacao", solicitacao);
        model.addAttribute("complemento", complemento);
        model.addAttribute("titulo", "Impressão da solicitação");

        return "impressao/visualizar";
    }

    @GetMapping("/impressao/{id}")
    public ResponseEntity<byte[]> imprimir(@PathVariable Long id, Authentication authentication) {
        var solicitacao = solicitacaoService.detalhe(id);
        var complemento = "TFD".equals(solicitacao.get("tipo_solicitacao")) ? solicitacaoService.tfd(id) : solicitacaoService.apac(id);
        boolean reimpressao = "IMPRESSA".equals(solicitacao.get("status"));
        PdfService.GeneratedPdf pdf = pdfService.gerar(solicitacao, complemento);
        solicitacaoService.registrarImpressao(id, currentUserService.id(authentication), solicitacao.get("tipo_solicitacao").toString(), reimpressao, pdf.path(), pdf.hash());

        Object numero = solicitacao.get("numero_solicitacao") != null
                ? solicitacao.get("numero_solicitacao")
                : solicitacao.get("numero_protocolo");

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                        .filename(numero + ".pdf").build().toString())
                .body(pdf.bytes());
    }
}
