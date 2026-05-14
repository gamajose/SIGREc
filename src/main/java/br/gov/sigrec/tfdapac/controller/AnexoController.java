package br.gov.sigrec.tfdapac.controller;

import br.gov.sigrec.tfdapac.service.CurrentUserService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.nio.file.Files;
import java.nio.file.Path;

@Controller
public class AnexoController {
    private final JdbcTemplate jdbcTemplate;
    private final CurrentUserService currentUserService;
    private final Path storagePath;

    public AnexoController(JdbcTemplate jdbcTemplate,
                           CurrentUserService currentUserService,
                           @Value("${sigrec.storage-path}") String storagePath) {
        this.jdbcTemplate = jdbcTemplate;
        this.currentUserService = currentUserService;
        this.storagePath = Path.of(storagePath);
    }

    @PostMapping("/solicitacoes/{id}/anexos")
    public String upload(@PathVariable Long id,
                         @RequestParam String tipo_documento,
                         @RequestParam MultipartFile arquivo,
                         Authentication auth,
                         RedirectAttributes ra) throws Exception {
        if (arquivo.isEmpty()) {
            ra.addFlashAttribute("erro", "Selecione um arquivo.");
            return "redirect:/solicitacoes/" + id;
        }
        String original = StringUtils.cleanPath(arquivo.getOriginalFilename() == null ? "anexo" : arquivo.getOriginalFilename());
        Path dir = storagePath.resolve("anexos").resolve(id.toString());
        Files.createDirectories(dir);
        Path destino = dir.resolve(System.currentTimeMillis() + "-" + original);
        arquivo.transferTo(destino);
        jdbcTemplate.update("""
                insert into regulacao_tfd.solicitacao_anexos
                (solicitacao_id, tipo_documento, nome_arquivo, caminho_storage, content_type, tamanho, usuario_upload)
                values (?, ?, ?, ?, ?, ?, ?)
                """, id, tipo_documento, original, destino.toAbsolutePath().toString(),
                arquivo.getContentType(), arquivo.getSize(), currentUserService.id(auth));
        jdbcTemplate.update("""
                insert into regulacao_tfd.solicitacao_historico
                (solicitacao_id, usuario_id, setor, acao, observacao)
                values (?, ?, 'Solicitante/Regulacao', 'ANEXO', ?)
                """, id, currentUserService.id(auth), tipo_documento + ": " + original);
        ra.addFlashAttribute("ok", "Anexo registrado.");
        return "redirect:/solicitacoes/" + id;
    }

    @PreAuthorize("hasAnyRole('ADMIN','REGULACAO','AUTORIZADOR','AUDITORIA')")
    @GetMapping("/anexos/{id}")
    public ResponseEntity<FileSystemResource> baixar(@PathVariable Long id) {
        var row = jdbcTemplate.queryForMap("select * from regulacao_tfd.solicitacao_anexos where id = ?", id);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(row.get("content_type") == null ? "application/octet-stream" : row.get("content_type").toString()))
                .body(new FileSystemResource(row.get("caminho_storage").toString()));
    }
}
