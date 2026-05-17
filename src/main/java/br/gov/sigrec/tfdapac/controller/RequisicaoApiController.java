package br.gov.sigrec.tfdapac.controller;

import br.gov.sigrec.tfdapac.service.CurrentUserService;
import br.gov.sigrec.tfdapac.service.SolicitacaoService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/requisicoes")
public class RequisicaoApiController {
    private final SolicitacaoService solicitacaoService;
    private final CurrentUserService currentUserService;

    public RequisicaoApiController(SolicitacaoService solicitacaoService, CurrentUserService currentUserService) {
        this.solicitacaoService = solicitacaoService;
        this.currentUserService = currentUserService;
    }

    @GetMapping
    public ResponseEntity<?> list(@RequestParam(defaultValue = "") String status,
                                  @RequestParam(defaultValue = "") String tipo,
                                  @RequestParam(defaultValue = "") String q,
                                  Authentication auth) {
        return ResponseEntity.ok(solicitacaoService.filaDoUsuario(
                status,
                tipo,
                q,
                currentUserService.id(auth),
                currentUserService.unidadeId(auth),
                currentUserService.isAdmin(auth)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> detail(@PathVariable Long id, Authentication auth) {
        if (!canAccess(id, auth)) {
            return ResponseEntity.status(403).body(Map.of("erro", "Acesso negado"));
        }
        Map<String, Object> body = new LinkedHashMap<>(solicitacaoService.detalhe(id));
        body.put("historico", solicitacaoService.historico(id));
        body.put("anexos", solicitacaoService.anexos(id));
        return ResponseEntity.ok(body);
    }

    @PostMapping
    public ResponseEntity<?> create(@RequestBody Map<String, String> body, Authentication auth) {
        Long id = solicitacaoService.criar(body, currentUserService.id(auth));
        return ResponseEntity.ok(Map.of("id", id, "mensagem", "Requisicao criada"));
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable Long id, @RequestBody Map<String, String> body, Authentication auth) {
        if (!canAccess(id, auth)) {
            return ResponseEntity.status(403).body(Map.of("erro", "Acesso negado"));
        }
        solicitacaoService.atualizar(id, body, currentUserService.id(auth));
        return ResponseEntity.ok(Map.of("id", id, "mensagem", "Requisicao atualizada"));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> status(@PathVariable Long id, @RequestBody Map<String, String> body, Authentication auth) {
        solicitacaoService.mudarStatus(id, body.get("status"), body.get("observacao"), currentUserService.id(auth), body);
        return ResponseEntity.ok(Map.of("id", id, "status", body.get("status")));
    }

    @GetMapping("/{id}/historico")
    public ResponseEntity<?> history(@PathVariable Long id, Authentication auth) {
        if (!canAccess(id, auth)) {
            return ResponseEntity.status(403).body(Map.of("erro", "Acesso negado"));
        }
        return ResponseEntity.ok(solicitacaoService.historico(id));
    }

    private boolean canAccess(Long id, Authentication auth) {
        return solicitacaoService.podeAcessar(
                id,
                currentUserService.id(auth),
                currentUserService.unidadeId(auth),
                currentUserService.isAdmin(auth));
    }
}
