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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class ImpressaoController {
    private final SolicitacaoService solicitacaoService;
    private final PdfService pdfService;
    private final CurrentUserService currentUserService;

    public ImpressaoController(SolicitacaoService solicitacaoService, PdfService pdfService, CurrentUserService currentUserService) {
        this.solicitacaoService = solicitacaoService;
        this.pdfService = pdfService;
        this.currentUserService = currentUserService;
    }

    @GetMapping("/impressao/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'REGULADOR', 'SOLICITANTE') or @desktopPrintTokenService.isValid(#id, #desktopToken)")
    public ResponseEntity<byte[]> imprimir(@PathVariable Long id,
                                           @RequestParam(name = "desktopToken", required = false) String desktopToken,
                                           Authentication authentication) {
        var solicitacao = solicitacaoService.detalhe(id);
        var complemento = "TFD".equals(solicitacao.get("tipo_solicitacao")) ? solicitacaoService.tfd(id) : solicitacaoService.apac(id);
        boolean reimpressao = "IMPRESSA".equals(solicitacao.get("status"));
        PdfService.GeneratedPdf pdf = pdfService.gerar(solicitacao, complemento);
        Long usuarioId = usuarioLogadoId(authentication);
        solicitacaoService.registrarImpressao(id, usuarioId, solicitacao.get("tipo_solicitacao").toString(), reimpressao, pdf.path(), pdf.hash());

        Object numero = solicitacao.get("numero_solicitacao") != null
                ? solicitacao.get("numero_solicitacao")
                : solicitacao.get("numero_protocolo");

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.inline()
                        .filename(numero + ".pdf").build().toString())
                .body(pdf.bytes());
    }

    private Long usuarioLogadoId(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }
        boolean anonymous = authentication.getAuthorities().stream()
                .anyMatch(authority -> "ROLE_ANONYMOUS".equals(authority.getAuthority()));
        return anonymous ? null : currentUserService.id(authentication);
    }
}
