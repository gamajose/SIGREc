package br.gov.sigrec.tfdapac.controller;

import br.gov.sigrec.tfdapac.service.AuditService;
import br.gov.sigrec.tfdapac.service.JwtService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthApiController {
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final AuditService auditService;

    public AuthApiController(AuthenticationManager authenticationManager, JwtService jwtService, AuditService auditService) {
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.auditService = auditService;
    }

    @PostMapping("/token")
    public ResponseEntity<Map<String, Object>> token(@RequestBody LoginRequest request, HttpServletRequest httpRequest) {
        authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(request.username(), request.password()));
        auditService.login(request.username(), true, httpRequest);
        return ResponseEntity.ok(Map.of(
                "token_type", "Bearer",
                "access_token", jwtService.generate(request.username())
        ));
    }

    public record LoginRequest(String username, String password) {
    }
}
