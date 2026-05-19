package br.gov.sigrec.tfdapac.controller;

import br.gov.sigrec.tfdapac.service.CurrentUserService;
import br.gov.sigrec.tfdapac.service.NotificationService;
import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class TicketController {
    private final JdbcTemplate jdbcTemplate;
    private final CurrentUserService currentUserService;
    private final NotificationService notificationService;

    @Value("${sigrec.uploads.tickets-dir:uploads/tickets}")
    private String ticketsUploadDir;

    public TicketController(JdbcTemplate jdbcTemplate,
            CurrentUserService currentUserService,
            NotificationService notificationService) {
        this.jdbcTemplate = jdbcTemplate;
        this.currentUserService = currentUserService;
        this.notificationService = notificationService;
    }

    @GetMapping("/suporte")
    public String suporte(Model model, Authentication auth) {
        Long usuarioId = currentUserService.id(auth);

        model.addAttribute("tickets", jdbcTemplate.queryForList("""
                select *
                from regulacao_tfd.tickets_suporte
                where usuario_id = ?
                order by criado_em desc
                """, usuarioId));

        return "tickets/suporte";
    }

    @PostMapping("/suporte")
    public String abrir(@RequestParam String assunto,
            @RequestParam(required = false) String mensagem,
            @RequestParam(required = false) String descricaoHtml,
            @RequestParam(required = false, defaultValue = "NORMAL") String prioridade,
            @RequestParam(required = false) String categoria,
            @RequestParam(required = false, name = "anexos") MultipartFile[] anexos,
            Authentication auth,
            RedirectAttributes ra) {
        Long usuarioId = currentUserService.id(auth);

        String email = jdbcTemplate.queryForObject(
                "select email from regulacao_tfd.usuarios where id = ?",
                String.class,
                usuarioId);

        Long id = jdbcTemplate.queryForObject("""
                insert into regulacao_tfd.tickets_suporte
                    (usuario_id, assunto, mensagem, descricao_html, prioridade, categoria, email_destino)
                values (?, ?, ?, ?, ?, ?, ?)
                returning id
                """,
                Long.class,
                usuarioId,
                assunto,
                mensagem,
                descricaoHtml,
                prioridade,
                categoria,
                email);

        jdbcTemplate.update("""
                update regulacao_tfd.tickets_suporte
                set numero_ticket = 'TCK-' || lpad(id::text, 6, '0')
                where id = ?
                """, id);

        salvarAnexos(id, anexos);

        ra.addFlashAttribute("ok", "Solicitação enviada para o suporte. Ticket: TCK-" + String.format("%06d", id));
        return "redirect:/suporte";
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/tickets")
    public String tickets(@RequestParam(required = false) String numero,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String usuario,
            @RequestParam(required = false) String dataInicio,
            @RequestParam(required = false) String dataFim,
            Model model) {
        StringBuilder sql = new StringBuilder("""
                select t.*, u.username, u.nome, u.email,
                       (
                           select count(*)
                           from regulacao_tfd.ticket_anexos a
                           where a.ticket_id = t.id
                       ) as total_anexos
                from regulacao_tfd.tickets_suporte t
                join regulacao_tfd.usuarios u on u.id = t.usuario_id
                where 1 = 1
                                """);

        List<Object> params = new ArrayList<>();

        if (numero != null && !numero.isBlank()) {
            sql.append("""
                    and (
                        upper(t.numero_ticket) like upper(?)
                        or cast(t.id as text) like ?
                    )
                    """);
            params.add("%" + numero.trim() + "%");
            params.add("%" + numero.trim().replace("TCK-", "") + "%");
        }

        if (status != null && !status.isBlank()) {
            sql.append(" and t.status = ? ");
            params.add(status.trim());
        }

        if (usuario != null && !usuario.isBlank()) {
            sql.append("""
                    and (
                        upper(u.username) like upper(?)
                        or upper(u.nome) like upper(?)
                        or upper(u.email) like upper(?)
                    )
                    """);
            String filtroUsuario = "%" + usuario.trim() + "%";
            params.add(filtroUsuario);
            params.add(filtroUsuario);
            params.add(filtroUsuario);
        }

        if (dataInicio != null && !dataInicio.isBlank()) {
            sql.append(" and t.criado_em::date >= ?::date ");
            params.add(dataInicio.trim());
        }

        if (dataFim != null && !dataFim.isBlank()) {
            sql.append(" and t.criado_em::date <= ?::date ");
            params.add(dataFim.trim());
        }

        sql.append("""
                order by
                    case t.prioridade
                        when 'URGENTE' then 1
                        when 'ALTA' then 2
                        when 'NORMAL' then 3
                        else 9
                    end,
                    case t.status
                        when 'ABERTO' then 1
                        when 'EM_ANALISE' then 2
                        when 'RESPONDIDO' then 3
                        when 'RESOLVIDO' then 4
                        when 'FECHADO' then 5
                        else 9
                    end,
                    t.criado_em desc
                """);

        List<Map<String, Object>> tickets = jdbcTemplate.queryForList(sql.toString(), params.toArray());

        for (Map<String, Object> ticket : tickets) {
            Object ticketId = ticket.get("id");
            if (ticketId != null) {
                ticket.put("anexos", anexosDoTicket(((Number) ticketId).longValue()));
            }
        }

        model.addAttribute("tickets", tickets);

        Integer ticketsAbertos = jdbcTemplate.queryForObject("""
                select count(*)
                from regulacao_tfd.tickets_suporte
                where status = 'ABERTO'
                """, Integer.class);

        model.addAttribute("ticketsAbertos", ticketsAbertos == null ? 0 : ticketsAbertos);

        return "tickets/list";
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/tickets/{id}")
    public String detalhe(@PathVariable Long id, Model model) {
        Map<String, Object> ticket = jdbcTemplate.queryForMap("""
                select t.*, u.username, u.nome, u.email
                from regulacao_tfd.tickets_suporte t
                join regulacao_tfd.usuarios u on u.id = t.usuario_id
                where t.id = ?
                """, id);

        model.addAttribute("ticket", ticket);
        model.addAttribute("anexos", anexosDoTicket(id));

        return "tickets/detalhe";
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/tickets/{id}/status")
    public String alterarStatus(@PathVariable Long id,
            @RequestParam String status,
            RedirectAttributes ra) {
        List<String> permitidos = List.of("ABERTO", "EM_ANALISE", "RESPONDIDO", "RESOLVIDO", "FECHADO");

        if (!permitidos.contains(status)) {
            ra.addFlashAttribute("erro", "Status informado é inválido.");
            return "redirect:/tickets/" + id;
        }

        jdbcTemplate.update("""
                update regulacao_tfd.tickets_suporte
                set status = ?, atualizado_em = current_timestamp
                where id = ?
                """, status, id);

        ra.addFlashAttribute("ok", "Status do ticket atualizado para " + status + ".");
        return "redirect:/tickets/" + id;
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/tickets/anexos/{anexoId}")
    public ResponseEntity<Resource> baixarAnexo(@PathVariable Long anexoId) {
        Map<String, Object> anexo = jdbcTemplate.queryForMap("""
                select a.*
                from regulacao_tfd.ticket_anexos a
                join regulacao_tfd.tickets_suporte t on t.id = a.ticket_id
                where a.id = ?
                """, anexoId);

        try {
            Path arquivo = Paths.get(anexo.get("caminho_arquivo").toString()).toAbsolutePath().normalize();
            Resource resource = new UrlResource(arquivo.toUri());

            if (!resource.exists() || !resource.isReadable()) {
                return ResponseEntity.notFound().build();
            }

            String nomeOriginal = anexo.get("nome_original") == null
                    ? "anexo"
                    : anexo.get("nome_original").toString();
            String contentType = anexo.get("content_type") == null
                    ? MediaType.APPLICATION_OCTET_STREAM_VALUE
                    : anexo.get("content_type").toString();

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            ContentDisposition.inline().filename(nomeOriginal).build().toString())
                    .body(resource);
        } catch (MalformedURLException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/tickets/{id}/responder")
    public String responder(@PathVariable Long id,
            @RequestParam String resposta,
            Authentication auth,
            RedirectAttributes ra) {
        Long adminId = currentUserService.id(auth);

        var ticket = jdbcTemplate.queryForMap("""
                select t.*, u.email, u.username
                from regulacao_tfd.tickets_suporte t
                join regulacao_tfd.usuarios u on u.id = t.usuario_id
                where t.id = ?
                """, id);

        jdbcTemplate.update("""
                update regulacao_tfd.tickets_suporte
                set status = 'RESOLVIDO',
                    resposta = ?,
                    respondido_por = ?,
                    respondido_em = current_timestamp,
                    atualizado_em = current_timestamp
                where id = ?
                """, resposta, adminId, id);

        String numeroTicket = ticket.get("numero_ticket") == null
                ? "TCK-" + String.format("%06d", id)
                : ticket.get("numero_ticket").toString();

        notificationService.queueEmail(
                ticket.get("email") == null ? null : ticket.get("email").toString(),
                "SIGREc - Ticket resolvido " + numeroTicket,
                "Seu ticket foi resolvido.\n\nAssunto: " + ticket.get("assunto") + "\nResposta: " + resposta);

        ra.addFlashAttribute("ok", "Ticket " + numeroTicket + " respondido. E-mail colocado na fila de envio.");
        return "redirect:/tickets/" + id;
    }

    private List<Map<String, Object>> anexosDoTicket(Long ticketId) {
        return jdbcTemplate.queryForList("""
                select id, nome_original, content_type, tamanho_bytes, criado_em
                from regulacao_tfd.ticket_anexos
                where ticket_id = ?
                order by criado_em asc, id asc
                """, ticketId);
    }

    private void salvarAnexos(Long ticketId, MultipartFile[] anexos) {
        if (anexos == null || anexos.length == 0) {
            return;
        }

        try {
            Path baseDir = Paths.get(ticketsUploadDir).toAbsolutePath().normalize();
            Path ticketDir = baseDir.resolve(String.valueOf(ticketId));
            Files.createDirectories(ticketDir);

            for (MultipartFile anexo : anexos) {
                if (anexo == null || anexo.isEmpty()) {
                    continue;
                }

                String original = anexo.getOriginalFilename();
                if (original == null || original.isBlank()) {
                    original = "arquivo";
                }

                String nomeSeguro = original.replaceAll("[^a-zA-Z0-9._-]", "_");
                String nomeArquivo = UUID.randomUUID() + "_" + nomeSeguro;
                Path destino = ticketDir.resolve(nomeArquivo).normalize();

                if (!destino.startsWith(ticketDir)) {
                    throw new IOException("Caminho de anexo inválido.");
                }

                anexo.transferTo(destino.toFile());

                jdbcTemplate.update("""
                        insert into regulacao_tfd.ticket_anexos
                            (ticket_id, nome_arquivo, nome_original, content_type, tamanho_bytes, caminho_arquivo)
                        values (?, ?, ?, ?, ?, ?)
                        """,
                        ticketId,
                        nomeArquivo,
                        original,
                        anexo.getContentType(),
                        anexo.getSize(),
                        destino.toString());
            }
        } catch (IOException e) {
            throw new IllegalStateException("Falha ao salvar anexos do ticket.", e);
        }
    }
}
